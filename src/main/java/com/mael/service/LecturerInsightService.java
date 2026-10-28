package com.mael.service;

import com.mael.dto.lecturer.*;
import com.mael.dto.review.ReviewRequest;
import com.mael.entity.Lecturer;
import com.mael.entity.enums.ReviewDecision;
import com.mael.entity.enums.ReviewStatus;
import com.mael.exception.ApiException;
import com.mael.repository.QuestionRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Cổng giảng viên: trang tổng quan, lịch sử duyệt, duyệt hàng loạt và phân tích học tập
 * (ải nào khó, câu nào hay sai, chương nào nhiều học sinh yếu, học sinh nào cần hỗ trợ).
 */
@Service
@RequiredArgsConstructor
public class LecturerInsightService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Câu hỏi cần ít nhất ngần này lượt trả lời mới đưa vào danh sách "hay sai" (tránh 1 lượt sai = 100%) */
    private static final int MIN_ANSWERS = 3;

    private final JdbcTemplate jdbc;
    private final CurrentUser currentUser;
    private final ReviewService reviewService;
    private final QuestionRepository questionRepository;

    // ------------------------------- Tổng quan -------------------------------

    @Transactional
    public LecturerDashboard dashboard() {
        Lecturer me = currentUser.lecturer();
        String lid = me.getId();
        LocalDate today = LocalDate.now(VN);
        return new LecturerDashboard(
                me.getFullName(),
                count("select count(*) from lesson where review_status = 'PENDING_REVIEW'"),
                count("select count(*) from question where review_status = 'PENDING_REVIEW'"),
                count("select count(*) from content_report where status in ('OPEN', 'IN_REVIEW')"),
                count("select count(*) from lesson_material where uploaded_by = ? and ai_status = 'PROCESSING'", lid),
                count("select count(*) from content_review where lecturer_id = ?", lid),
                count("select count(*) from content_review where lecturer_id = ? and reviewed_at >= ?", lid, today.minusDays(6).atStartOfDay()),
                count("select count(*) from content_review where lecturer_id = ? and decision = 'APPROVED'", lid),
                count("select count(*) from content_review where lecturer_id = ? and decision = 'REJECTED'", lid),
                count("select count(*) from lesson_material where uploaded_by = ?", lid),
                count("select count(*) from student where last_active_date >= ?", today.minusDays(6)),
                history(true, 6),
                lessonStats(null, 5),
                hardestQuestions(null, 5));
    }

    // ------------------------------- Lịch sử duyệt -------------------------------

    @Transactional
    public List<ReviewHistoryItem> history(boolean mineOnly, int limit) {
        String lid = mineOnly ? currentUser.lecturer().getId() : null;
        String sql = """
                select r.review_id, r.lesson_id, r.question_id, r.decision, r.comment, r.reviewed_at, lec.full_name,
                       coalesce(l.title, q.content) title, coalesce(tl.name, tq.name) topic_name
                from content_review r
                join lecturer lec on lec.lecturer_id = r.lecturer_id
                left join lesson l on l.lesson_id = r.lesson_id left join topic tl on tl.topic_id = l.topic_id
                left join question q on q.question_id = r.question_id left join topic tq on tq.topic_id = q.topic_id
                """ + (lid != null ? " where r.lecturer_id = ?" : "") + " order by r.reviewed_at desc limit ?";
        Object[] args = lid != null ? new Object[]{lid, Math.min(limit, 200)} : new Object[]{Math.min(limit, 200)};
        return jdbc.query(sql, (rs, i) -> {
            boolean lesson = rs.getString("lesson_id") != null;
            return new ReviewHistoryItem(rs.getString("review_id"), lesson ? "LESSON" : "QUESTION",
                    lesson ? rs.getString("lesson_id") : rs.getString("question_id"),
                    rs.getString("title"), rs.getString("topic_name"),
                    ReviewDecision.valueOf(rs.getString("decision")), rs.getString("comment"),
                    rs.getString("full_name"), rs.getTimestamp("reviewed_at").toLocalDateTime());
        }, args);
    }

    // ------------------------------- Duyệt hàng loạt -------------------------------

    /** Trả về số nội dung đã xử lý. Bỏ qua mục không còn ở trạng thái chờ duyệt (người khác đã duyệt). */
    @Transactional
    public Map<String, Integer> bulkReview(BulkReviewRequest req) {
        if (req.decision() == ReviewDecision.REJECTED && (req.comment() == null || req.comment().isBlank())) {
            throw ApiException.badRequest("Vui lòng ghi góp ý khi từ chối");
        }
        ReviewRequest single = new ReviewRequest(req.decision(), req.comment());
        int done = 0, skipped = 0;
        for (String id : Optional.ofNullable(req.lessonIds()).orElse(List.of())) {
            if (isPending("lesson", "lesson_id", id)) { reviewService.reviewLesson(id, single); done++; } else skipped++;
        }
        for (String id : Optional.ofNullable(req.questionIds()).orElse(List.of())) {
            if (isPending("question", "question_id", id)) { reviewService.reviewQuestion(id, single); done++; } else skipped++;
        }
        return Map.of("done", done, "skipped", skipped);
    }

    private boolean isPending(String table, String idCol, String id) {
        return count("select count(*) from " + table + " where " + idCol + " = ? and review_status = 'PENDING_REVIEW'", id) > 0;
    }

    // ------------------------------- Phân tích học tập -------------------------------

    @Transactional(readOnly = true)
    public LearningAnalytics analytics(Integer grade) {
        LocalDate today = LocalDate.now(VN);
        String gradeStudent = grade == null ? "" : " and st.grade_level = " + grade.intValue();
        String gradeTopic = grade == null ? "" : " and lp.grade_level = " + grade.intValue();

        Double avgRate = jdbc.queryForObject("""
                select avg(sr.best_correct::float / sr.total_questions) from stage_result sr
                join lesson l on l.lesson_id = sr.lesson_id join topic t on t.topic_id = l.topic_id
                join learning_path lp on lp.path_id = t.path_id
                where sr.total_questions > 0""" + gradeTopic, Double.class);
        LearningAnalytics.Overview overview = new LearningAnalytics.Overview(
                count("select count(*) from student st where 1 = 1" + gradeStudent),
                count("select count(*) from student st where st.last_active_date >= ?" + gradeStudent, today.minusDays(6)),
                count("""
                        select coalesce(sum(sr.attempts), 0) from stage_result sr
                        left join lesson l on l.lesson_id = sr.lesson_id
                        join topic t on t.topic_id = coalesce(l.topic_id, sr.topic_id)
                        join learning_path lp on lp.path_id = t.path_id where 1 = 1""" + gradeTopic),
                count("""
                        select count(*) from arena_play a join topic t on t.topic_id = a.topic_id
                        join learning_path lp on lp.path_id = t.path_id where 1 = 1""" + gradeTopic),
                avgRate);

        List<TopicStat> topics = jdbc.query("""
                select t.topic_id, t.name, lp.grade_level,
                       (select count(*) from weakness w where w.topic_id = t.topic_id and w.status <> 'RESOLVED') weak,
                       (select avg(w.mastery_score) from weakness w where w.topic_id = t.topic_id and w.status <> 'RESOLVED') mastery,
                       (select count(*) from arena_play a where a.topic_id = t.topic_id) arena,
                       (select sum(a.correct)::float / nullif(sum(a.total), 0) from arena_play a where a.topic_id = t.topic_id) arena_acc,
                       (select count(*) from stage_result sr where sr.topic_id = t.topic_id and sr.stars > 0) boss
                from topic t join learning_path lp on lp.path_id = t.path_id
                where lp.is_active""" + gradeTopic + " order by lp.grade_level, t.order_index", (rs, i) -> new TopicStat(
                rs.getString("topic_id"), rs.getString("name"), rs.getInt("grade_level"), rs.getLong("weak"),
                rs.getObject("mastery") == null ? null : rs.getDouble("mastery"), rs.getLong("arena"),
                (Double) rs.getObject("arena_acc"), rs.getLong("boss")));

        return new LearningAnalytics(grade, overview, topics, lessonStats(grade, 50), hardestQuestions(grade, 15),
                studentsNeedHelp(grade));
    }

    /** Ải có tỉ lệ đúng thấp nhất trước (ải chưa ai chơi xếp cuối) */
    private List<LessonStat> lessonStats(Integer grade, int limit) {
        String gradeTopic = grade == null ? "" : " and lp.grade_level = " + grade.intValue();
        return jdbc.query("""
                select l.lesson_id, l.title, t.name topic_name, lp.grade_level,
                       count(sr.stage_result_id) players, coalesce(sum(sr.attempts), 0) attempts,
                       coalesce(avg(sr.stars), 0) avg_stars,
                       avg(case when sr.total_questions > 0 then sr.best_correct::float / sr.total_questions end) rate
                from lesson l join topic t on t.topic_id = l.topic_id join learning_path lp on lp.path_id = t.path_id
                join stage_result sr on sr.lesson_id = l.lesson_id
                where l.review_status = 'PUBLISHED'""" + gradeTopic + " " + """
                 group by l.lesson_id, l.title, t.name, lp.grade_level
                 order by rate asc nulls last, players desc limit ?""", (rs, i) -> new LessonStat(
                rs.getString("lesson_id"), rs.getString("title"), rs.getString("topic_name"), rs.getInt("grade_level"),
                rs.getLong("players"), rs.getLong("attempts"), rs.getDouble("avg_stars"), (Double) rs.getObject("rate")), limit);
    }

    private List<QuestionAdminView> hardestQuestions(Integer grade, int limit) {
        String gradeTopic = grade == null ? "" : " and lp.grade_level = " + grade.intValue();
        List<String> ids = jdbc.queryForList("""
                select q.question_id from question q join topic t on t.topic_id = q.topic_id
                join learning_path lp on lp.path_id = t.path_id
                where q.review_status = 'PUBLISHED' and q.answered_count >= ? and q.wrong_count > 0""" + gradeTopic + " " + """
                 order by q.wrong_count::float / q.answered_count desc, q.answered_count desc limit ?""",
                String.class, MIN_ANSWERS, limit);
        Map<String, com.mael.entity.Question> byId = new HashMap<>();
        questionRepository.findAllById(ids).forEach(q -> byId.put(q.getId(), q));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(ContentManageService::toQuestionView).toList();
    }

    private List<StudentAlert> studentsNeedHelp(Integer grade) {
        String gradeStudent = grade == null ? "" : " and st.grade_level = " + grade.intValue();
        return jdbc.query("""
                select st.student_id, st.full_name, st.grade_level, st.last_active_date, st.streak_days,
                       count(w.weakness_id) weak, avg(w.mastery_score) mastery,
                       string_agg(t.name, '||' order by w.mastery_score) topic_names
                from student st join weakness w on w.student_id = st.student_id and w.status <> 'RESOLVED'
                join topic t on t.topic_id = w.topic_id
                join user_account u on u.user_id = st.user_id and u.status = 'ACTIVE'
                where 1 = 1""" + gradeStudent + " " + """
                 group by st.student_id
                 having count(w.weakness_id) >= 1
                 order by count(w.weakness_id) desc, avg(w.mastery_score) asc limit 12""", (rs, i) -> {
            Date last = rs.getDate("last_active_date");
            String names = rs.getString("topic_names");
            return new StudentAlert(rs.getString("student_id"), rs.getString("full_name"), rs.getInt("grade_level"),
                    rs.getLong("weak"), rs.getObject("mastery") == null ? null : rs.getDouble("mastery"),
                    names == null ? List.of() : Arrays.stream(names.split("\\|\\|")).limit(3).toList(),
                    last == null ? null : last.toLocalDate(), rs.getInt("streak_days"));
        });
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }
}

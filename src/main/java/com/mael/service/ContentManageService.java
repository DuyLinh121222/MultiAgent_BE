package com.mael.service;

import com.mael.dto.lecturer.*;
import com.mael.entity.*;
import com.mael.entity.enums.*;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Kho nội dung dùng chung cho giảng viên và admin: cây Lớp → Chương → Bài, ngân hàng câu hỏi,
 * soạn / sửa / gỡ (lưu trữ) bài và câu hỏi.
 * Giảng viên là người duyệt nên nội dung giảng viên tự soạn được xuất bản luôn nếu chọn "Xuất bản".
 */
@Service
@RequiredArgsConstructor
public class ContentManageService {

    private final JdbcTemplate jdbc;
    private final LearningPathRepository pathRepository;
    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final ContentReviewRepository reviewRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUser currentUser;

    // ------------------------------- Cây nội dung -------------------------------

    @Transactional(readOnly = true)
    public List<ContentTree> tree(Integer grade) {
        // Đếm câu hỏi theo bài và theo chương (câu ải trùm = không gắn bài)
        Map<String, long[]> byLesson = new HashMap<>();
        Map<String, Long> bossByTopic = new HashMap<>();
        jdbc.query("""
                select lesson_id, topic_id,
                       count(*) filter (where review_status = 'PUBLISHED') pub,
                       count(*) filter (where review_status = 'PENDING_REVIEW') pending,
                       count(*) filter (where review_status <> 'ARCHIVED') total
                from question group by lesson_id, topic_id""", rs -> {
            String lessonId = rs.getString("lesson_id");
            if (lessonId == null) {
                bossByTopic.merge(rs.getString("topic_id"), rs.getLong("pub"), Long::sum);
            } else {
                long[] c = byLesson.computeIfAbsent(lessonId, k -> new long[3]);
                c[0] += rs.getLong("pub"); c[1] += rs.getLong("pending"); c[2] += rs.getLong("total");
            }
        });
        Map<String, Object[]> plays = new HashMap<>();
        jdbc.query("""
                select lesson_id, count(*) players,
                       avg(case when total_questions > 0 then best_correct::float / total_questions end) rate
                from stage_result where lesson_id is not null group by lesson_id""", rs -> {
            plays.put(rs.getString("lesson_id"), new Object[]{rs.getLong("players"), (Double) rs.getObject("rate")});
        });

        List<LearningPath> paths = pathRepository.findByActiveTrueOrderByGradeLevelAsc().stream()
                .filter(p -> grade == null || p.getGradeLevel().equals(grade)).toList();
        List<ContentTree> result = new ArrayList<>();
        for (LearningPath path : paths) {
            List<ContentTree.TopicNode> topics = new ArrayList<>();
            for (Topic t : topicRepository.findByLearningPathIdOrderByOrderIndexAsc(path.getId())) {
                List<ContentTree.LessonNode> lessons = t.getLessons().stream()
                        .sorted(Comparator.comparing(Lesson::getOrderIndex))
                        .map(l -> {
                            long[] c = byLesson.getOrDefault(l.getId(), new long[3]);
                            Object[] p = plays.getOrDefault(l.getId(), new Object[]{0L, null});
                            return new ContentTree.LessonNode(l.getId(), l.getTitle(), l.getOrderIndex(), l.getReviewStatus(),
                                    c[0], c[1], c[2], (Long) p[0], (Double) p[1]);
                        }).toList();
                topics.add(new ContentTree.TopicNode(t.getId(), t.getName(), t.getDescription(), t.getOrderIndex(),
                        t.getSemester() == null ? 1 : t.getSemester(), bossByTopic.getOrDefault(t.getId(), 0L), lessons));
            }
            result.add(new ContentTree(path.getId(), path.getName(), path.getGradeLevel(), topics));
        }
        return result;
    }

    // ------------------------------- Chương -------------------------------

    @Transactional
    public ContentTree.TopicNode createTopic(TopicUpsertRequest req) {
        if (req.pathId() == null || req.pathId().isBlank()) throw ApiException.badRequest("Chọn lớp");
        LearningPath path = pathRepository.findById(req.pathId()).orElseThrow(() -> ApiException.notFound("Không tìm thấy lộ trình"));
        Integer max = jdbc.queryForObject("select coalesce(max(order_index), 0) from topic where path_id = ?", Integer.class, path.getId());
        Topic t = topicRepository.save(Topic.builder()
                .learningPath(path)
                .name(req.name().trim())
                .description(req.description())
                .semester(req.semester() == null ? 1 : req.semester())
                .orderIndex((max == null ? 0 : max) + 1)
                .build());
        return new ContentTree.TopicNode(t.getId(), t.getName(), t.getDescription(), t.getOrderIndex(), t.getSemester(), 0, List.of());
    }

    @Transactional
    public void updateTopic(String topicId, TopicUpsertRequest req) {
        Topic t = topic(topicId);
        t.setName(req.name().trim());
        t.setDescription(req.description());
        if (req.semester() != null) t.setSemester(req.semester());
    }

    // ------------------------------- Bài (ải) -------------------------------

    @Transactional(readOnly = true)
    public List<LessonAdminView> lessons(ReviewStatus status) {
        return lessonRepository.findByReviewStatusIn(EnumSet.of(status == null ? ReviewStatus.PENDING_REVIEW : status))
                .stream().map(ContentManageService::toLessonView).toList();
    }

    @Transactional(readOnly = true)
    public LessonAdminView lesson(String lessonId) {
        return toLessonView(lessonEntity(lessonId));
    }

    @Transactional
    public LessonAdminView createLesson(LessonUpsertRequest req) {
        Topic topic = topic(req.topicId());
        boolean publish = Boolean.TRUE.equals(req.publish());
        Lesson lesson = Lesson.builder()
                .topic(topic)
                .title(req.title().trim())
                .content(req.content())
                .videoUrl(blankToNull(req.videoUrl()))
                .durationMinutes(req.durationMinutes())
                .misconceptions(cleanMisconceptions(req.misconceptions()))
                .orderIndex(lessonRepository.findMaxOrderIndex(topic.getId()) + 1)
                .createdBy(userAccountRepository.getReferenceById(currentUser.userId()))
                .reviewStatus(publish ? ReviewStatus.PUBLISHED : ReviewStatus.DRAFT)
                .build();
        if (publish) markReviewed(lesson);
        lesson = lessonRepository.save(lesson);
        if (publish) saveReview(lesson, null, "Giảng viên tự soạn và xuất bản");
        return toLessonView(lesson);
    }

    /** Sửa bài. Bài đang chờ duyệt / bị từ chối mà chọn "Xuất bản" thì coi như duyệt luôn. */
    @Transactional
    public LessonAdminView updateLesson(String lessonId, LessonUpsertRequest req) {
        Lesson lesson = lessonEntity(lessonId);
        if (!lesson.getTopic().getId().equals(req.topicId())) {
            Topic topic = topic(req.topicId());
            lesson.setTopic(topic);
            lesson.setOrderIndex(lessonRepository.findMaxOrderIndex(topic.getId()) + 1);
        }
        lesson.setTitle(req.title().trim());
        lesson.setContent(req.content());
        lesson.setVideoUrl(blankToNull(req.videoUrl()));
        lesson.setDurationMinutes(req.durationMinutes());
        if (req.misconceptions() != null) lesson.setMisconceptions(cleanMisconceptions(req.misconceptions()));
        if (Boolean.TRUE.equals(req.publish()) && lesson.getReviewStatus() != ReviewStatus.PUBLISHED) {
            boolean wasPending = lesson.getReviewStatus() == ReviewStatus.PENDING_REVIEW;
            lesson.setReviewStatus(ReviewStatus.PUBLISHED);
            markReviewed(lesson);
            saveReview(lesson, null, wasPending ? "Sửa và duyệt" : "Giảng viên tự soạn và xuất bản");
        }
        return toLessonView(lesson);
    }

    @Transactional
    public LessonAdminView setLessonArchived(String lessonId, boolean archived) {
        Lesson lesson = lessonEntity(lessonId);
        if (archived) {
            lesson.setReviewStatus(ReviewStatus.ARCHIVED);
        } else {
            if (lesson.getReviewStatus() != ReviewStatus.ARCHIVED) throw ApiException.badRequest("Bài này không ở trạng thái lưu trữ");
            lesson.setReviewStatus(ReviewStatus.PUBLISHED);
            markReviewed(lesson);
        }
        return toLessonView(lesson);
    }

    // ------------------------------- Câu hỏi -------------------------------

    /**
     * Ngân hàng câu hỏi.
     *
     * @param sort "wrong" = câu học sinh sai nhiều nhất trước, mặc định mới nhất trước
     */
    @Transactional(readOnly = true)
    public List<QuestionAdminView> questions(String topicId, String lessonId, ReviewStatus status, String q, String sort) {
        StringBuilder jpql = new StringBuilder("""
                select q.question_id from question q where 1 = 1""");
        List<Object> args = new ArrayList<>();
        if (lessonId != null && !lessonId.isBlank()) {
            if ("BOSS".equals(lessonId)) {
                jpql.append(" and q.lesson_id is null");
            } else {
                jpql.append(" and q.lesson_id = ?"); args.add(lessonId);
            }
        }
        if (topicId != null && !topicId.isBlank()) { jpql.append(" and q.topic_id = ?"); args.add(topicId); }
        if (status != null) {
            jpql.append(" and q.review_status = ?"); args.add(status.name());
        } else {
            jpql.append(" and q.review_status <> 'ARCHIVED'");
        }
        if (q != null && !q.isBlank()) { jpql.append(" and lower(q.content) like ?"); args.add("%" + q.trim().toLowerCase(Locale.ROOT) + "%"); }
        jpql.append("wrong".equals(sort)
                ? " order by case when q.answered_count = 0 then 1 else 0 end, q.wrong_count::float / greatest(q.answered_count, 1) desc, q.answered_count desc"
                : " order by q.created_at desc");
        jpql.append(" limit 300");
        List<String> ids = jdbc.queryForList(jpql.toString(), String.class, args.toArray());
        Map<String, Question> byId = new HashMap<>();
        questionRepository.findAllById(ids).forEach(x -> byId.put(x.getId(), x));
        return ids.stream().map(byId::get).filter(Objects::nonNull).map(ContentManageService::toQuestionView).toList();
    }

    @Transactional
    public QuestionAdminView createQuestion(QuestionUpsertRequest req) {
        Topic topic = topic(req.topicId());
        Lesson lesson = resolveLesson(req.lessonId(), topic);
        List<String> options = validateQuestion(req);
        boolean publish = Boolean.TRUE.equals(req.publish());
        Question q = Question.builder()
                .topic(topic)
                .lesson(lesson)
                .content(req.content().trim())
                .questionType(req.questionType())
                .options(options)
                .correctAnswer(req.correctAnswer().trim())
                .explanation(req.explanation())
                .difficulty(req.difficulty() != null ? req.difficulty() : Difficulty.MEDIUM)
                .source(ContentSource.MANUAL)
                .createdBy(userAccountRepository.getReferenceById(currentUser.userId()))
                .reviewStatus(publish ? ReviewStatus.PUBLISHED : ReviewStatus.DRAFT)
                .build();
        if (publish) {
            q.setReviewedBy(currentUser.lecturer());
            q.setReviewedAt(LocalDateTime.now());
        }
        q = questionRepository.save(q);
        if (publish) saveReview(null, q, "Giảng viên tự soạn và xuất bản");
        return toQuestionView(q);
    }

    /** Sửa câu hỏi (kể cả câu đang chờ duyệt: sửa xong chọn Xuất bản = duyệt luôn) */
    @Transactional
    public QuestionAdminView updateQuestion(String questionId, QuestionUpsertRequest req) {
        Question q = questionEntity(questionId);
        Topic topic = topic(req.topicId());
        List<String> options = validateQuestion(req);
        q.setTopic(topic);
        q.setLesson(resolveLesson(req.lessonId(), topic));
        q.setContent(req.content().trim());
        q.setQuestionType(req.questionType());
        q.setOptions(options);
        q.setCorrectAnswer(req.correctAnswer().trim());
        q.setExplanation(req.explanation());
        if (req.difficulty() != null) q.setDifficulty(req.difficulty());
        if (Boolean.TRUE.equals(req.publish()) && q.getReviewStatus() != ReviewStatus.PUBLISHED) {
            boolean wasPending = q.getReviewStatus() == ReviewStatus.PENDING_REVIEW;
            q.setReviewStatus(ReviewStatus.PUBLISHED);
            q.setReviewedBy(currentUser.lecturer());
            q.setReviewedAt(LocalDateTime.now());
            saveReview(null, q, wasPending ? "Sửa và duyệt" : "Giảng viên tự soạn và xuất bản");
        }
        return toQuestionView(q);
    }

    @Transactional
    public QuestionAdminView setQuestionArchived(String questionId, boolean archived) {
        Question q = questionEntity(questionId);
        if (archived) {
            q.setReviewStatus(ReviewStatus.ARCHIVED);
        } else {
            if (q.getReviewStatus() != ReviewStatus.ARCHIVED) throw ApiException.badRequest("Câu hỏi này không ở trạng thái lưu trữ");
            q.setReviewStatus(ReviewStatus.PUBLISHED);
            q.setReviewedBy(currentUser.lecturer());
            q.setReviewedAt(LocalDateTime.now());
        }
        return toQuestionView(q);
    }

    /** Xoá thống kê đúng/sai sau khi sửa câu bị lỗi (số liệu cũ không còn đúng với câu mới) */
    @Transactional
    public QuestionAdminView resetStats(String questionId) {
        Question q = questionEntity(questionId);
        q.setAnsweredCount(0);
        q.setWrongCount(0);
        return toQuestionView(q);
    }

    // ------------------------------------------------------------------

    /** Kiểm tra câu trắc nghiệm: đủ 2 lựa chọn, đáp án đúng phải là 1 lựa chọn (A, B...) */
    private static List<String> validateQuestion(QuestionUpsertRequest req) {
        List<String> options = req.options() == null ? List.of()
                : req.options().stream().filter(o -> o != null && !o.isBlank()).map(String::trim).toList();
        if (req.questionType() == QuestionType.SHORT_ANSWER) return List.of();
        if (options.size() < 2) throw ApiException.badRequest("Câu trắc nghiệm cần ít nhất 2 lựa chọn");
        if (options.size() > 6) throw ApiException.badRequest("Tối đa 6 lựa chọn");
        Set<String> letters = new HashSet<>();
        for (String o : options) {
            if (!o.matches("^[A-F]\\..+")) throw ApiException.badRequest("Mỗi lựa chọn bắt đầu bằng chữ cái và dấu chấm, ví dụ \"A. x = 2\"");
            letters.add(o.substring(0, 1));
        }
        for (String part : req.correctAnswer().toUpperCase(Locale.ROOT).replaceAll("[^A-F]", " ").trim().split("\\s+")) {
            if (part.isEmpty() || !letters.contains(part)) {
                throw ApiException.badRequest("Đáp án đúng phải là chữ cái của một lựa chọn (ví dụ B)");
            }
        }
        return options;
    }

    private Lesson resolveLesson(String lessonId, Topic topic) {
        if (lessonId == null || lessonId.isBlank() || "BOSS".equals(lessonId)) return null;
        Lesson lesson = lessonEntity(lessonId);
        if (!lesson.getTopic().getId().equals(topic.getId())) throw ApiException.badRequest("Bài không thuộc chương đã chọn");
        return lesson;
    }

    private void markReviewed(Lesson lesson) {
        lesson.setReviewedBy(currentUser.lecturer());
        lesson.setReviewedAt(LocalDateTime.now());
    }

    private void saveReview(Lesson lesson, Question question, String comment) {
        reviewRepository.save(ContentReview.builder()
                .lecturer(currentUser.lecturer())
                .lesson(lesson)
                .question(question)
                .decision(ReviewDecision.APPROVED)
                .comment(comment)
                .build());
    }

    private static List<Misconception> cleanMisconceptions(List<Misconception> list) {
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list.stream().filter(m -> m != null && m.wrong() != null && !m.wrong().isBlank()).toList());
    }

    private Topic topic(String id) {
        return topicRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy chương"));
    }

    private Lesson lessonEntity(String id) {
        return lessonRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài"));
    }

    private Question questionEntity(String id) {
        return questionRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy câu hỏi"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static LessonAdminView toLessonView(Lesson l) {
        return new LessonAdminView(l.getId(), l.getTopic().getId(), l.getTopic().getName(),
                l.getTopic().getLearningPath().getGradeLevel(), l.getTitle(), l.getContent(), l.getVideoUrl(),
                l.getDurationMinutes(), l.getMisconceptions(), l.getOrderIndex(), l.getReviewStatus(),
                l.getReviewedBy() != null ? l.getReviewedBy().getFullName() : null, l.getReviewedAt(), l.getUpdatedAt());
    }

    static QuestionAdminView toQuestionView(Question q) {
        int answered = q.getAnsweredCount() == null ? 0 : q.getAnsweredCount();
        int wrong = q.getWrongCount() == null ? 0 : q.getWrongCount();
        return new QuestionAdminView(q.getId(), q.getContent(), q.getQuestionType(), q.getOptions(), q.getCorrectAnswer(),
                q.getExplanation(), q.getDifficulty(), q.getSource(), q.getTopic().getId(), q.getTopic().getName(),
                q.getTopic().getLearningPath().getGradeLevel(),
                q.getLesson() != null ? q.getLesson().getId() : null, q.getLesson() != null ? q.getLesson().getTitle() : null,
                q.getReviewStatus(), answered, wrong, answered == 0 ? null : (double) wrong / answered,
                q.getReviewedBy() != null ? q.getReviewedBy().getFullName() : null, q.getCreatedAt());
    }
}

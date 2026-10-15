package com.mael.service;

import com.mael.dto.review.*;
import com.mael.entity.*;
import com.mael.entity.enums.ReportStatus;
import com.mael.entity.enums.ReviewDecision;
import com.mael.entity.enums.ReviewStatus;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Cổng giảng viên: duyệt bài giảng / câu hỏi và xử lý báo lỗi của học sinh.
 * Duyệt (APPROVED) thì nội dung được xuất bản ngay (PUBLISHED) cho học sinh và AI dùng làm ngữ cảnh.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final ContentReviewRepository reviewRepository;
    private final ContentReportRepository reportRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<PendingLessonView> pendingLessons() {
        return lessonRepository.findByReviewStatusIn(EnumSet.of(ReviewStatus.PENDING_REVIEW)).stream()
                .map(l -> new PendingLessonView(l.getId(), l.getTitle(), l.getContent(), l.getTopic().getName(),
                        l.getTopic().getLearningPath().getGradeLevel(), l.getReviewStatus(), l.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingQuestionView> pendingQuestions() {
        return questionRepository.findByReviewStatusIn(EnumSet.of(ReviewStatus.PENDING_REVIEW)).stream()
                .map(q -> new PendingQuestionView(q.getId(), q.getContent(), q.getQuestionType(), q.getOptions(),
                        q.getCorrectAnswer(), q.getExplanation(), q.getDifficulty(), q.getSource(),
                        q.getTopic().getName(), q.getReviewStatus(), q.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void reviewLesson(String lessonId, ReviewRequest req) {
        Lecturer lecturer = currentUser.lecturer();
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài giảng"));
        requirePending(lesson.getReviewStatus());
        validateComment(req);
        lesson.setReviewStatus(req.decision() == ReviewDecision.APPROVED ? ReviewStatus.PUBLISHED : ReviewStatus.REJECTED);
        lesson.setReviewedBy(lecturer);
        lesson.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(ContentReview.builder()
                .lecturer(lecturer).lesson(lesson).decision(req.decision()).comment(req.comment()).build());
    }

    @Transactional
    public void reviewQuestion(String questionId, ReviewRequest req) {
        Lecturer lecturer = currentUser.lecturer();
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy câu hỏi"));
        requirePending(q.getReviewStatus());
        validateComment(req);
        q.setReviewStatus(req.decision() == ReviewDecision.APPROVED ? ReviewStatus.PUBLISHED : ReviewStatus.REJECTED);
        q.setReviewedBy(lecturer);
        q.setReviewedAt(LocalDateTime.now());
        reviewRepository.save(ContentReview.builder()
                .lecturer(lecturer).question(q).decision(req.decision()).comment(req.comment()).build());
    }

    @Transactional(readOnly = true)
    public List<ReportView> openReports() {
        return reportRepository.findByStatusInOrderByCreatedAtDesc(EnumSet.of(ReportStatus.OPEN, ReportStatus.IN_REVIEW))
                .stream().map(ReviewService::toReportView).toList();
    }

    /**
     * Xử lý báo lỗi. Nếu lỗi nằm ở bài giảng / câu hỏi gốc và giảng viên xác nhận (RESOLVED),
     * nội dung đó quay về PENDING_REVIEW để sửa và duyệt lại.
     */
    @Transactional
    public ReportView handleReport(String reportId, HandleReportRequest req) {
        Lecturer lecturer = currentUser.lecturer();
        ContentReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy báo cáo"));
        r.setStatus(req.status());
        r.setResolutionNote(req.resolutionNote());
        r.setHandledBy(lecturer);
        if (req.status() == ReportStatus.RESOLVED || req.status() == ReportStatus.DISMISSED) {
            r.setResolvedAt(LocalDateTime.now());
        }
        if (req.status() == ReportStatus.RESOLVED) {
            if (r.getLesson() != null) r.getLesson().setReviewStatus(ReviewStatus.PENDING_REVIEW);
            if (r.getQuestion() != null) r.getQuestion().setReviewStatus(ReviewStatus.PENDING_REVIEW);
        }
        return toReportView(r);
    }

    static ReportView toReportView(ContentReport r) {
        String type;
        String id;
        String preview;
        if (r.getLesson() != null) {
            type = "LESSON"; id = r.getLesson().getId(); preview = r.getLesson().getTitle();
        } else if (r.getQuestion() != null) {
            type = "QUESTION"; id = r.getQuestion().getId(); preview = r.getQuestion().getContent();
        } else if (r.getMessage() != null) {
            type = "MESSAGE"; id = r.getMessage().getId(); preview = r.getMessage().getContent();
        } else {
            type = "SOLUTION"; id = r.getSolution().getId(); preview = r.getSolution().getFinalAnswer();
        }
        return new ReportView(r.getId(), r.getStudent().getFullName(), type, id, preview, r.getReason(),
                r.getDescription(), r.getStatus(), r.getResolutionNote(), r.getCreatedAt(), r.getResolvedAt());
    }

    private static void requirePending(ReviewStatus status) {
        if (status != ReviewStatus.PENDING_REVIEW) {
            throw ApiException.badRequest("Nội dung này không ở trạng thái chờ duyệt");
        }
    }

    private static void validateComment(ReviewRequest req) {
        if (req.decision() == ReviewDecision.REJECTED && (req.comment() == null || req.comment().isBlank())) {
            throw ApiException.badRequest("Vui lòng ghi góp ý khi từ chối");
        }
    }
}

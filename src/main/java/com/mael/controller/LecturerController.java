package com.mael.controller;

import com.mael.dto.review.*;
import com.mael.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cổng giảng viên (role LECTURER hoặc ADMIN): duyệt nội dung, xử lý báo lỗi.
 */
@RestController
@RequestMapping("/api/lecturer")
@RequiredArgsConstructor
public class LecturerController {

    private final ReviewService reviewService;
    private final com.mael.service.DocumentService documentService;
    private final com.mael.service.LecturerInsightService insightService;

    // ---------------- Tổng quan, lịch sử duyệt, phân tích học tập ----------------

    @GetMapping("/dashboard")
    public com.mael.dto.lecturer.LecturerDashboard dashboard() {
        return insightService.dashboard();
    }

    /** mine = true: chỉ lịch sử của tôi; false: của tất cả giảng viên */
    @GetMapping("/reviews")
    public List<com.mael.dto.lecturer.ReviewHistoryItem> reviews(@RequestParam(defaultValue = "true") boolean mine,
                                                                 @RequestParam(defaultValue = "50") int limit) {
        return insightService.history(mine, limit);
    }

    @PostMapping("/review-bulk")
    public java.util.Map<String, Integer> bulkReview(@Valid @RequestBody com.mael.dto.lecturer.BulkReviewRequest req) {
        return insightService.bulkReview(req);
    }

    @GetMapping("/analytics")
    public com.mael.dto.lecturer.LearningAnalytics analytics(@RequestParam(required = false) Integer grade) {
        return insightService.analytics(grade);
    }

    // ---------------- Tài liệu PDF -> AI soạn câu hỏi / mini game ----------------

    @GetMapping("/topics")
    public List<com.mael.dto.document.TopicOption> topics() {
        return documentService.topics();
    }

    @GetMapping("/documents")
    public List<com.mael.dto.document.DocumentView> documents() {
        return documentService.myDocuments();
    }

    @GetMapping("/documents/{id}")
    public com.mael.dto.document.DocumentView document(@PathVariable String id) {
        return documentService.get(id);
    }

    /** Tải PDF: lessonId = gắn vào bài có sẵn; bỏ trống lessonId thì tạo bài mới trong chương topicId */
    @PostMapping(value = "/documents", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public com.mael.dto.document.DocumentView upload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                                     @RequestParam(required = false) String topicId,
                                                     @RequestParam(required = false) String lessonId,
                                                     @RequestParam(required = false) String title) {
        return documentService.upload(file, topicId, lessonId, title);
    }

    @PostMapping("/documents/{id}/reprocess")
    public com.mael.dto.document.DocumentView reprocess(@PathVariable String id) {
        return documentService.reprocess(id);
    }

    @PostMapping("/documents/{id}/approve-all")
    public com.mael.dto.document.DocumentView approveAll(@PathVariable String id) {
        return documentService.approveAll(id);
    }

    @GetMapping("/lessons/pending")
    public List<PendingLessonView> pendingLessons() {
        return reviewService.pendingLessons();
    }

    @GetMapping("/questions/pending")
    public List<PendingQuestionView> pendingQuestions() {
        return reviewService.pendingQuestions();
    }

    @PostMapping("/lessons/{lessonId}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reviewLesson(@PathVariable String lessonId, @Valid @RequestBody ReviewRequest req) {
        reviewService.reviewLesson(lessonId, req);
    }

    @PostMapping("/questions/{questionId}/review")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reviewQuestion(@PathVariable String questionId, @Valid @RequestBody ReviewRequest req) {
        reviewService.reviewQuestion(questionId, req);
    }

    @GetMapping("/reports")
    public List<ReportView> reports() {
        return reviewService.openReports();
    }

    @PatchMapping("/reports/{reportId}")
    public ReportView handleReport(@PathVariable String reportId, @Valid @RequestBody HandleReportRequest req) {
        return reviewService.handleReport(reportId, req);
    }
}

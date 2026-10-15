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

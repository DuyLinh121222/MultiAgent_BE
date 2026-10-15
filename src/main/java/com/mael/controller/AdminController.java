package com.mael.controller;

import com.mael.dto.review.CreateLessonRequest;
import com.mael.dto.review.CreateQuestionRequest;
import com.mael.service.AdminContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin soạn nội dung và gửi giảng viên duyệt.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminContentService adminContentService;

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createLesson(@Valid @RequestBody CreateLessonRequest req) {
        return adminContentService.createLesson(req);
    }

    @PostMapping("/lessons/{lessonId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitLesson(@PathVariable String lessonId) {
        adminContentService.submitLessonForReview(lessonId);
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createQuestion(@Valid @RequestBody CreateQuestionRequest req) {
        return adminContentService.createQuestion(req);
    }

    @PostMapping("/questions/{questionId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitQuestion(@PathVariable String questionId) {
        adminContentService.submitQuestionForReview(questionId);
    }
}

package com.mael.controller;

import com.mael.dto.lecturer.*;
import com.mael.entity.enums.ReviewStatus;
import com.mael.service.ContentManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Kho nội dung dùng chung cho giảng viên và admin (/api/manage/** chặn ở SecurityConfig):
 * cây Lớp → Chương → Bài, ngân hàng câu hỏi, soạn / sửa / lưu trữ.
 */
@RestController
@RequestMapping("/api/manage")
@RequiredArgsConstructor
public class ContentManageController {

    private final ContentManageService contentService;

    @GetMapping("/content/tree")
    public List<ContentTree> tree(@RequestParam(required = false) Integer grade) {
        return contentService.tree(grade);
    }

    // ---------------- Chương ----------------

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public ContentTree.TopicNode createTopic(@Valid @RequestBody TopicUpsertRequest req) {
        return contentService.createTopic(req);
    }

    @PutMapping("/topics/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateTopic(@PathVariable String topicId, @Valid @RequestBody TopicUpsertRequest req) {
        contentService.updateTopic(topicId, req);
    }

    // ---------------- Bài (ải) ----------------

    /** Danh sách bài theo trạng thái (mặc định: chờ duyệt) */
    @GetMapping("/lessons")
    public List<LessonAdminView> lessons(@RequestParam(required = false) ReviewStatus status) {
        return contentService.lessons(status);
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonAdminView lesson(@PathVariable String lessonId) {
        return contentService.lesson(lessonId);
    }

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public LessonAdminView createLesson(@Valid @RequestBody LessonUpsertRequest req) {
        return contentService.createLesson(req);
    }

    @PutMapping("/lessons/{lessonId}")
    public LessonAdminView updateLesson(@PathVariable String lessonId, @Valid @RequestBody LessonUpsertRequest req) {
        return contentService.updateLesson(lessonId, req);
    }

    @PostMapping("/lessons/{lessonId}/archive")
    public LessonAdminView archiveLesson(@PathVariable String lessonId) {
        return contentService.setLessonArchived(lessonId, true);
    }

    @PostMapping("/lessons/{lessonId}/restore")
    public LessonAdminView restoreLesson(@PathVariable String lessonId) {
        return contentService.setLessonArchived(lessonId, false);
    }

    // ---------------- Câu hỏi ----------------

    /** lessonId = BOSS: câu của ải trùm (không gắn bài). sort = wrong: câu sai nhiều nhất trước */
    @GetMapping("/questions")
    public List<QuestionAdminView> questions(@RequestParam(required = false) String topicId,
                                             @RequestParam(required = false) String lessonId,
                                             @RequestParam(required = false) ReviewStatus status,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String sort) {
        return contentService.questions(topicId, lessonId, status, q, sort);
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionAdminView createQuestion(@Valid @RequestBody QuestionUpsertRequest req) {
        return contentService.createQuestion(req);
    }

    @PutMapping("/questions/{questionId}")
    public QuestionAdminView updateQuestion(@PathVariable String questionId, @Valid @RequestBody QuestionUpsertRequest req) {
        return contentService.updateQuestion(questionId, req);
    }

    @PostMapping("/questions/{questionId}/archive")
    public QuestionAdminView archiveQuestion(@PathVariable String questionId) {
        return contentService.setQuestionArchived(questionId, true);
    }

    @PostMapping("/questions/{questionId}/restore")
    public QuestionAdminView restoreQuestion(@PathVariable String questionId) {
        return contentService.setQuestionArchived(questionId, false);
    }

    @PostMapping("/questions/{questionId}/reset-stats")
    public QuestionAdminView resetStats(@PathVariable String questionId) {
        return contentService.resetStats(questionId);
    }
}

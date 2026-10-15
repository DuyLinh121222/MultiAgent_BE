package com.mael.controller;

import com.mael.dto.quiz.*;
import com.mael.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/quizzes")
    public List<QuizSummary> quizzes() {
        return quizService.availableQuizzes();
    }

    /** Bắt đầu làm bài: trả về đề (không kèm đáp án) */
    @PostMapping("/quizzes/{quizId}/attempts")
    @ResponseStatus(HttpStatus.CREATED)
    public StartAttemptResponse start(@PathVariable String quizId) {
        return quizService.startAttempt(quizId);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public AttemptResult submit(@PathVariable String attemptId, @Valid @RequestBody SubmitAttemptRequest req) {
        return quizService.submit(attemptId, req);
    }

    @GetMapping("/weaknesses")
    public List<WeaknessView> weaknesses() {
        return quizService.myWeaknesses();
    }
}

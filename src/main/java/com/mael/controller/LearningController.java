package com.mael.controller;

import com.mael.dto.learning.*;
import com.mael.service.DashboardService;
import com.mael.service.LearningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LearningController {

    private final LearningService learningService;
    private final DashboardService dashboardService;

    /** Trang chủ & lộ trình của học sinh đang đăng nhập */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.myDashboard();
    }

    @GetMapping("/learning-paths")
    public List<LearningPathSummary> paths() {
        return learningService.listPaths();
    }

    /** Lộ trình theo khối lớp của học sinh */
    @GetMapping("/learning-paths/me")
    public LearningPathDetail myPath() {
        return learningService.myPath();
    }

    @GetMapping("/learning-paths/{pathId}")
    public LearningPathDetail path(@PathVariable String pathId) {
        return learningService.pathDetail(pathId);
    }

    @GetMapping("/lessons/{lessonId}")
    public LessonDetail lesson(@PathVariable String lessonId) {
        return learningService.lessonDetail(lessonId);
    }

    @PutMapping("/lessons/{lessonId}/progress")
    public LessonSummary updateProgress(@PathVariable String lessonId,
                                        @Valid @RequestBody ProgressUpdateRequest req) {
        return learningService.updateProgress(lessonId, req.progressPercent());
    }
}

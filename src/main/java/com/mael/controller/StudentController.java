package com.mael.controller;

import com.mael.dto.common.AchievementView;
import com.mael.dto.common.NotificationView;
import com.mael.dto.review.ReportRequest;
import com.mael.dto.review.ReportView;
import com.mael.service.AchievementService;
import com.mael.service.NotificationService;
import com.mael.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Thông báo, thành tích, báo lỗi nội dung của học sinh.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class StudentController {

    private final NotificationService notificationService;
    private final AchievementService achievementService;
    private final ReportService reportService;

    @GetMapping("/notifications")
    public List<NotificationView> notifications() {
        return notificationService.myNotifications();
    }

    @PatchMapping("/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable String id) {
        notificationService.markRead(id);
    }

    @PatchMapping("/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead() {
        notificationService.markAllRead();
    }

    @GetMapping("/achievements")
    public List<AchievementView> achievements() {
        return achievementService.myAchievements();
    }

    @PostMapping("/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportView report(@Valid @RequestBody ReportRequest req) {
        return reportService.create(req);
    }
}

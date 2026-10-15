package com.mael.dto.learning;

import com.mael.dto.plan.SubscriptionView;
import com.mael.dto.quiz.WeaknessView;

import java.util.List;

/**
 * Dữ liệu cho Trang chủ & Lộ trình.
 */
public record DashboardResponse(
        String fullName,
        Integer gradeLevel,
        SubscriptionView subscription,
        int completedLessons,
        int totalLessons,
        int totalPoints,
        int achievementCount,
        long unreadNotifications,
        ContinueLesson continueLesson,
        List<WeaknessView> weaknesses,
        LearningPathDetail learningPath
) {
}

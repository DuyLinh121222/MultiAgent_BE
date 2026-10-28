package com.mael.dto.admin;

import com.mael.dto.payment.PaymentView;
import com.mael.dto.plan.SubscriptionView;
import com.mael.dto.quiz.WeaknessView;
import com.mael.entity.enums.LearningGoal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Chi tiết 1 tài khoản: phần student hoặc lecturer có dữ liệu tuỳ role */
public record AdminUserDetail(
        AdminUserRow user,
        StudentInfo student,
        LecturerInfo lecturer
) {
    public record StudentInfo(
            String studentId,
            LocalDate dateOfBirth,
            LearningGoal learningGoal,
            boolean onboarded,
            int coins,
            int xp,
            int level,
            int streakDays,
            int bestStreak,
            long stagesCleared,
            long totalStars,
            long arenaPlays,
            long discussionSessions,
            List<SubscriptionView> subscriptions,
            List<PaymentView> payments,
            List<WeaknessView> weaknesses
    ) {
    }

    public record LecturerInfo(
            String lecturerId,
            String specialization,
            String academicDegree,
            String workplace,
            long reviewsTotal,
            long approved,
            long rejected,
            long documents,
            long reportsHandled,
            LocalDateTime lastReviewAt
    ) {
    }
}

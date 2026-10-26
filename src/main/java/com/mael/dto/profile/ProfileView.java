package com.mael.dto.profile;

import com.mael.dto.plan.SubscriptionView;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Hồ sơ học sinh + gói đang dùng (biết khi nào hết hạn dùng thử / hết gói).
 */
public record ProfileView(
        String email,
        String fullName,
        String phone,
        Integer gradeLevel,
        LocalDate dateOfBirth,
        String learningGoal,
        String activePetCode,
        LocalDateTime joinedAt,
        SubscriptionView subscription,
        List<SubscriptionView> history
) {
}

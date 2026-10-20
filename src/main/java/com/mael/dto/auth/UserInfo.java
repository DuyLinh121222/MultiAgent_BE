package com.mael.dto.auth;

import com.mael.dto.plan.SubscriptionView;
import com.mael.entity.enums.Role;

/**
 * Thông tin người dùng trả về cho FE sau khi đăng nhập hoặc gọi /api/auth/me.
 * studentId / gradeLevel / subscription / onboarded / coins / xp / activePetCode chỉ có với học sinh,
 * lecturerId chỉ có với giảng viên.
 * onboarded = false: FE đưa học sinh vào màn chào mừng 3 bước trước khi vào bản đồ ải.
 */
public record UserInfo(
        String userId,
        String email,
        Role role,
        String fullName,
        String studentId,
        String lecturerId,
        Integer gradeLevel,
        String avatarUrl,
        SubscriptionView subscription,
        Boolean onboarded,
        Integer coins,
        Integer xp,
        String activePetCode
) {
}

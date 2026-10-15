package com.mael.dto.auth;

import com.mael.dto.plan.SubscriptionView;
import com.mael.entity.enums.Role;

/**
 * Thông tin người dùng trả về cho FE sau khi đăng nhập hoặc gọi /api/auth/me.
 * studentId / gradeLevel / subscription chỉ có với học sinh, lecturerId chỉ có với giảng viên.
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
        SubscriptionView subscription
) {
}

package com.mael.dto.admin;

import com.mael.entity.enums.AccountStatus;
import com.mael.entity.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 1 dòng trong bảng quản lý người dùng. Trường nào không áp dụng cho role đó thì null
 * (ví dụ gradeLevel / planName chỉ có ở học sinh, specialization chỉ có ở giảng viên).
 */
public record AdminUserRow(
        String userId,
        String email,
        Role role,
        AccountStatus status,
        String fullName,
        Integer gradeLevel,
        String phone,
        Integer xp,
        Integer streakDays,
        LocalDate lastActiveDate,
        String planName,
        LocalDateTime planEndDate,
        Boolean trial,
        String specialization,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}

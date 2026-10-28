package com.mael.dto.admin;

import com.mael.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin tạo tài khoản giảng viên hoặc quản trị viên. Bỏ trống password thì hệ thống tự sinh mật khẩu tạm.
 */
public record CreateStaffRequest(
        @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") String email,
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
        @NotNull(message = "Chọn vai trò") Role role,
        @Size(min = 6, max = 64, message = "Mật khẩu từ 6 đến 64 ký tự") String password,
        @Size(max = 150) String specialization,
        @Size(max = 100) String academicDegree,
        @Size(max = 255) String workplace
) {
}

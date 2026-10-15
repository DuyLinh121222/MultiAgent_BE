package com.mael.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") String email,
        @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự") String password,
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150) String fullName,
        @NotNull(message = "Vui lòng chọn lớp") @Min(value = 6, message = "Lớp từ 6 đến 9") @Max(value = 9, message = "Lớp từ 6 đến 9") Integer gradeLevel,
        @Size(max = 20) String phone
) {
}

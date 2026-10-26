package com.mael.dto.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới") @Size(min = 6, max = 72, message = "Mật khẩu mới từ 6 đến 72 kí tự") String newPassword
) {
}

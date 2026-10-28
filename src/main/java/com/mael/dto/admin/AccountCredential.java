package com.mael.dto.admin;

/** Trả về sau khi tạo tài khoản / cấp lại mật khẩu: admin gửi mật khẩu tạm cho người dùng */
public record AccountCredential(String userId, String email, String tempPassword) {
}

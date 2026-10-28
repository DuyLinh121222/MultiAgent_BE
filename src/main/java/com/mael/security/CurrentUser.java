package com.mael.security;

import com.mael.entity.Lecturer;
import com.mael.entity.Student;
import com.mael.entity.UserAccount;
import com.mael.entity.enums.Role;
import com.mael.exception.ApiException;
import com.mael.repository.LecturerRepository;
import com.mael.repository.StudentRepository;
import com.mael.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Lấy user / student / lecturer đang đăng nhập.
 */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final UserAccountRepository userAccountRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public UserPrincipal principal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập");
        }
        return principal;
    }

    public String userId() {
        return principal().getUserId();
    }

    public boolean isAdmin() {
        return principal().getRole() == Role.ADMIN;
    }

    public Student student() {
        return studentRepository.findByUserId(userId())
                .orElseThrow(() -> ApiException.forbidden("Chức năng này chỉ dành cho học sinh"));
    }

    /**
     * Hồ sơ giảng viên của người đang đăng nhập.
     * Admin cũng được duyệt nội dung / tải tài liệu: lần đầu dùng sẽ tự tạo hồ sơ giảng viên gắn với tài khoản admin
     * (để ghi "người duyệt" vào CONTENT_REVIEW). Hồ sơ này không hiện trong danh sách giảng viên vì role vẫn là ADMIN.
     */
    public Lecturer lecturer() {
        return lecturerRepository.findByUserId(userId()).orElseGet(() -> {
            if (!isAdmin()) throw ApiException.forbidden("Chức năng này chỉ dành cho giảng viên");
            UserAccount admin = userAccountRepository.findById(userId()).orElseThrow();
            // ON CONFLICT: 2 request cùng lúc (menu + trang) không bị lỗi trùng hồ sơ
            jdbc.update("""
                    insert into lecturer (lecturer_id, user_id, full_name, specialization)
                    values (gen_random_uuid()::text, ?, ?, 'Quản trị hệ thống')
                    on conflict (user_id) do nothing""",
                    admin.getId(), admin.getDisplayName() != null ? admin.getDisplayName() : "Quản trị viên");
            return lecturerRepository.findByUserId(admin.getId()).orElseThrow();
        });
    }

    /** Tên hiển thị của người đang đăng nhập (ghi nhật ký) */
    public String displayName() {
        UserAccount u = userAccountRepository.findById(userId()).orElseThrow();
        return switch (u.getRole()) {
            case ADMIN -> u.getDisplayName() != null ? u.getDisplayName() : "Quản trị viên";
            case LECTURER -> lecturerRepository.findByUserId(u.getId()).map(Lecturer::getFullName).orElse(u.getEmail());
            case STUDENT -> studentRepository.findByUserId(u.getId()).map(Student::getFullName).orElse(u.getEmail());
        };
    }
}

package com.mael.security;

import com.mael.entity.Lecturer;
import com.mael.entity.Student;
import com.mael.exception.ApiException;
import com.mael.repository.LecturerRepository;
import com.mael.repository.StudentRepository;
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

    public Student student() {
        return studentRepository.findByUserId(userId())
                .orElseThrow(() -> ApiException.forbidden("Chức năng này chỉ dành cho học sinh"));
    }

    public Lecturer lecturer() {
        return lecturerRepository.findByUserId(userId())
                .orElseThrow(() -> ApiException.forbidden("Chức năng này chỉ dành cho giảng viên"));
    }
}

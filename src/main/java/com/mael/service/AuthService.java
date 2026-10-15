package com.mael.service;

import com.mael.dto.auth.AuthResponse;
import com.mael.dto.auth.LoginRequest;
import com.mael.dto.auth.RegisterRequest;
import com.mael.dto.auth.UserInfo;
import com.mael.entity.Lecturer;
import com.mael.entity.Student;
import com.mael.entity.UserAccount;
import com.mael.entity.enums.NotificationType;
import com.mael.entity.enums.Role;
import com.mael.exception.ApiException;
import com.mael.repository.LecturerRepository;
import com.mael.repository.StudentRepository;
import com.mael.repository.UserAccountRepository;
import com.mael.security.CurrentUser;
import com.mael.security.JwtService;
import com.mael.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    /**
     * Học sinh tự đăng ký. Tạo USER_ACCOUNT + STUDENT và tặng 7 ngày dùng thử.
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        UserAccount account = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.STUDENT)
                .lastLoginAt(LocalDateTime.now())
                .build());
        Student student = studentRepository.save(Student.builder()
                .user(account)
                .fullName(req.fullName().trim())
                .gradeLevel(req.gradeLevel())
                .phone(req.phone())
                .build());
        subscriptionService.startTrial(student);
        notificationService.notify(student, NotificationType.SYSTEM, "Chào mừng bạn đến với MAEL!",
                "Bạn có 7 ngày dùng thử miễn phí. Vào Phòng học nhóm AI để bắt đầu nhé.", null);

        UserPrincipal principal = UserPrincipal.from(account);
        return new AuthResponse(jwtService.generateToken(principal), jwtService.getExpirationMs(), buildUserInfo(account));
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email().trim(), req.password()));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        UserAccount account = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        account.setLastLoginAt(LocalDateTime.now());
        return new AuthResponse(jwtService.generateToken(principal), jwtService.getExpirationMs(), buildUserInfo(account));
    }

    @Transactional(readOnly = true)
    public UserInfo me() {
        UserAccount account = userAccountRepository.findById(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        return buildUserInfo(account);
    }

    private UserInfo buildUserInfo(UserAccount account) {
        return switch (account.getRole()) {
            case STUDENT -> {
                Student s = studentRepository.findByUserId(account.getId())
                        .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ học sinh"));
                var sub = subscriptionService.currentSubscription(s.getId()).map(DtoMapper::toSubscriptionView).orElse(null);
                yield new UserInfo(account.getId(), account.getEmail(), account.getRole(), s.getFullName(),
                        s.getId(), null, s.getGradeLevel(), s.getAvatarUrl(), sub);
            }
            case LECTURER -> {
                Lecturer l = lecturerRepository.findByUserId(account.getId())
                        .orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ giảng viên"));
                yield new UserInfo(account.getId(), account.getEmail(), account.getRole(), l.getFullName(),
                        null, l.getId(), null, null, null);
            }
            case ADMIN -> new UserInfo(account.getId(), account.getEmail(), account.getRole(), "Quản trị viên",
                    null, null, null, null, null);
        };
    }
}

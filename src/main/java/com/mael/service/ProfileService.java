package com.mael.service;

import com.mael.dto.profile.ChangePasswordRequest;
import com.mael.dto.profile.ProfileView;
import com.mael.dto.profile.UpdateProfileRequest;
import com.mael.entity.Student;
import com.mael.entity.UserAccount;
import com.mael.exception.ApiException;
import com.mael.repository.StudentRepository;
import com.mael.repository.UserAccountRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Học sinh xem / sửa hồ sơ, đổi mật khẩu, xem gói và ngày hết hạn.
 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CurrentUser currentUser;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final SubscriptionService subscriptionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public ProfileView myProfile() {
        return toView(currentUser.student());
    }

    @Transactional
    public ProfileView update(UpdateProfileRequest req) {
        Student s = currentUser.student();
        s.setFullName(req.fullName().trim());
        s.setPhone(req.phone() == null || req.phone().isBlank() ? null : req.phone().trim());
        s.setGradeLevel(req.gradeLevel());
        s.setDateOfBirth(req.dateOfBirth());
        if (req.learningGoal() != null) s.setLearningGoal(req.learningGoal());
        studentRepository.save(s);
        return toView(s);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        UserAccount account = userAccountRepository.findById(currentUser.userId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        if (!passwordEncoder.matches(req.currentPassword(), account.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        if (req.currentPassword().equals(req.newPassword())) {
            throw ApiException.badRequest("Mật khẩu mới phải khác mật khẩu cũ");
        }
        account.setPasswordHash(passwordEncoder.encode(req.newPassword()));
    }

    private ProfileView toView(Student s) {
        var current = subscriptionService.currentSubscription(s.getId()).map(DtoMapper::toSubscriptionView).orElse(null);
        return new ProfileView(s.getUser().getEmail(), s.getFullName(), s.getPhone(), s.getGradeLevel(), s.getDateOfBirth(),
                s.getLearningGoal() == null ? null : s.getLearningGoal().name(), s.getActivePetCode(), s.getCreatedAt(),
                current, subscriptionService.historyOf(s.getId()));
    }
}

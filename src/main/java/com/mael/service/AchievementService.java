package com.mael.service;

import com.mael.dto.common.AchievementView;
import com.mael.entity.Student;
import com.mael.entity.StudentAchievement;
import com.mael.entity.enums.NotificationType;
import com.mael.repository.AchievementRepository;
import com.mael.repository.StudentAchievementRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    public static final String FIRST_LESSON = "FIRST_LESSON";
    public static final String BUG_HUNTER = "BUG_HUNTER";
    public static final String QUIZ_MASTER = "QUIZ_MASTER";
    public static final String PHOTO_SOLVER = "PHOTO_SOLVER";

    private final AchievementRepository achievementRepository;
    private final StudentAchievementRepository studentAchievementRepository;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    /**
     * Trao danh hiệu nếu học sinh chưa có. Trả về true nếu vừa mới đạt.
     */
    @Transactional
    public boolean award(Student student, String code) {
        if (studentAchievementRepository.existsByStudentIdAndAchievementCode(student.getId(), code)) {
            return false;
        }
        return achievementRepository.findAll().stream()
                .filter(a -> a.getCode().equals(code))
                .findFirst()
                .map(a -> {
                    studentAchievementRepository.save(StudentAchievement.builder()
                            .student(student).achievement(a).build());
                    notificationService.notify(student, NotificationType.ACHIEVEMENT,
                            "Bạn vừa đạt danh hiệu: " + a.getName(),
                            a.getDescription() + " (+" + a.getPoints() + " điểm)", null);
                    return true;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<AchievementView> myAchievements() {
        String studentId = currentUser.student().getId();
        Map<String, StudentAchievement> earned = studentAchievementRepository.findByStudentId(studentId).stream()
                .collect(Collectors.toMap(sa -> sa.getAchievement().getId(), Function.identity()));
        return achievementRepository.findAll().stream()
                .map(a -> {
                    StudentAchievement sa = earned.get(a.getId());
                    return new AchievementView(a.getId(), a.getCode(), a.getName(), a.getDescription(),
                            a.getIconUrl(), a.getPoints(), sa != null, sa != null ? sa.getEarnedAt() : null);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public int totalPoints(String studentId) {
        return studentAchievementRepository.findByStudentId(studentId).stream()
                .mapToInt(sa -> sa.getAchievement().getPoints()).sum();
    }

    @Transactional(readOnly = true)
    public int count(String studentId) {
        return studentAchievementRepository.findByStudentId(studentId).size();
    }
}

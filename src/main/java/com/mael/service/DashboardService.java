package com.mael.service;

import com.mael.dto.learning.ContinueLesson;
import com.mael.dto.learning.DashboardResponse;
import com.mael.dto.learning.LearningPathDetail;
import com.mael.dto.quiz.WeaknessView;
import com.mael.entity.Student;
import com.mael.entity.enums.ProgressStatus;
import com.mael.entity.enums.ReviewStatus;
import com.mael.entity.enums.WeaknessStatus;
import com.mael.repository.LearningPathRepository;
import com.mael.repository.LessonProgressRepository;
import com.mael.repository.NotificationRepository;
import com.mael.repository.WeaknessRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CurrentUser currentUser;
    private final LearningService learningService;
    private final LearningPathRepository pathRepository;
    private final LessonProgressRepository progressRepository;
    private final WeaknessRepository weaknessRepository;
    private final NotificationRepository notificationRepository;
    private final SubscriptionService subscriptionService;
    private final AchievementService achievementService;

    @Transactional(readOnly = true)
    public DashboardResponse myDashboard() {
        Student s = currentUser.student();

        LearningPathDetail path = pathRepository.findFirstByGradeLevelAndActiveTrue(s.getGradeLevel())
                .map(p -> learningService.buildPathDetail(p, s))
                .orElse(null);

        ContinueLesson cont = progressRepository.findRecent(s.getId()).stream()
                .filter(p -> p.getStatus() == ProgressStatus.IN_PROGRESS
                        && p.getLesson().getReviewStatus() == ReviewStatus.PUBLISHED)
                .findFirst()
                .map(p -> new ContinueLesson(p.getLesson().getId(), p.getLesson().getTitle(),
                        p.getLesson().getTopic().getName(), p.getProgressPercent()))
                .orElse(null);

        List<WeaknessView> weaknesses = weaknessRepository.findByStudentId(s.getId()).stream()
                .filter(w -> w.getStatus() != WeaknessStatus.RESOLVED)
                .map(DtoMapper::toWeaknessView)
                .toList();

        return new DashboardResponse(
                s.getFullName(),
                s.getGradeLevel(),
                subscriptionService.currentSubscription(s.getId()).map(DtoMapper::toSubscriptionView).orElse(null),
                path != null ? path.completedLessons() : 0,
                path != null ? path.totalLessons() : 0,
                achievementService.totalPoints(s.getId()),
                achievementService.count(s.getId()),
                notificationRepository.countByStudentIdAndReadFalse(s.getId()),
                cont,
                weaknesses,
                path);
    }
}

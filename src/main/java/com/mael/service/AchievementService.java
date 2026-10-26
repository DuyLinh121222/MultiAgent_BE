package com.mael.service;

import com.mael.dto.common.AchievementView;
import com.mael.entity.Achievement;
import com.mael.entity.StageResult;
import com.mael.entity.Student;
import com.mael.entity.StudentAchievement;
import com.mael.entity.enums.NotificationType;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Huy hiệu thành tích.
 *
 * - Huy hiệu có metric + target (bảng ACHIEVEMENT) được tự động mở khi chỉ số đạt mốc: gọi {@link #evaluate(Student)}
 *   sau mỗi lần qua ải, chơi Đấu trường, mua thú...
 * - Huy hiệu đặc biệt (metric = null) trao thủ công bằng {@link #award(Student, String)}: bắt lỗi AI, giải bằng ảnh...
 * - Mỗi huy hiệu mở được thưởng số xu bằng số điểm của huy hiệu.
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    public static final String FIRST_LESSON = "FIRST_LESSON";
    public static final String BUG_HUNTER = "BUG_HUNTER";
    public static final String QUIZ_MASTER = "QUIZ_MASTER";
    public static final String PHOTO_SOLVER = "PHOTO_SOLVER";

    /** Các chỉ số dùng để mở huy hiệu tự động */
    public enum Metric { STAGES, STARS, PERFECT, BOSSES, STREAK, PETS, LEVEL, COINS, DISCUSSIONS, ARENA_PLAYS, ARENA_COMBO, PHOTOS }

    private final AchievementRepository achievementRepository;
    private final StudentAchievementRepository studentAchievementRepository;
    private final StageResultRepository stageResultRepository;
    private final StudentPetRepository studentPetRepository;
    private final DiscussionSessionRepository discussionSessionRepository;
    private final ArenaPlayRepository arenaPlayRepository;
    private final PhotoProblemRepository photoProblemRepository;
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
                    grant(student, a);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Kiểm tra mọi huy hiệu tự động, mở những huy hiệu vừa đạt mốc. Trả về danh sách huy hiệu mới mở.
     */
    @Transactional
    public List<AchievementView> evaluate(Student student) {
        Set<String> owned = studentAchievementRepository.findByStudentId(student.getId()).stream()
                .map(sa -> sa.getAchievement().getId()).collect(Collectors.toSet());
        Map<Metric, Integer> metrics = null;
        List<AchievementView> unlocked = new ArrayList<>();
        for (Achievement a : sorted()) {
            if (a.getMetric() == null || a.getTarget() == null || owned.contains(a.getId())) continue;
            if (metrics == null) metrics = metrics(student);
            int value = metrics.getOrDefault(parse(a.getMetric()), 0);
            if (value >= a.getTarget()) {
                StudentAchievement sa = grant(student, a);
                unlocked.add(toView(a, sa, value));
            }
        }
        return unlocked;
    }

    @Transactional(readOnly = true)
    public List<AchievementView> myAchievements() {
        Student student = currentUser.student();
        Map<String, StudentAchievement> earned = studentAchievementRepository.findByStudentId(student.getId()).stream()
                .collect(Collectors.toMap(sa -> sa.getAchievement().getId(), Function.identity()));
        Map<Metric, Integer> metrics = metrics(student);
        return sorted().stream()
                .map(a -> toView(a, earned.get(a.getId()),
                        a.getMetric() == null ? 0 : metrics.getOrDefault(parse(a.getMetric()), 0)))
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

    // ------------------------------------------------------------------

    private StudentAchievement grant(Student student, Achievement a) {
        StudentAchievement sa = studentAchievementRepository.save(StudentAchievement.builder()
                .student(student).achievement(a).build());
        int coins = a.getPoints() == null ? 0 : a.getPoints();
        student.setCoins((student.getCoins() == null ? 0 : student.getCoins()) + coins);
        notificationService.notify(student, NotificationType.ACHIEVEMENT,
                "Bạn vừa đạt huy hiệu: " + a.getName(),
                a.getDescription() + " (+" + coins + " xu)", null);
        return sa;
    }

    private Map<Metric, Integer> metrics(Student s) {
        Map<Metric, Integer> m = new EnumMap<>(Metric.class);
        List<StageResult> results = stageResultRepository.findByStudentId(s.getId());
        m.put(Metric.STAGES, (int) results.stream().filter(r -> r.getLesson() != null && r.getStars() > 0).count());
        m.put(Metric.STARS, results.stream().mapToInt(StageResult::getStars).sum());
        m.put(Metric.PERFECT, (int) results.stream().filter(r -> r.getStars() == 3).count());
        m.put(Metric.BOSSES, (int) results.stream().filter(r -> r.getTopic() != null && r.getStars() > 0).count());
        m.put(Metric.STREAK, Math.max(nz(s.getBestStreak()), nz(s.getStreakDays())));
        m.put(Metric.PETS, studentPetRepository.findByStudentId(s.getId()).size());
        m.put(Metric.LEVEL, nz(s.getXp()) / 100 + 1);
        m.put(Metric.COINS, nz(s.getCoins()));
        m.put(Metric.DISCUSSIONS, (int) discussionSessionRepository.countByStudentId(s.getId()));
        m.put(Metric.ARENA_PLAYS, (int) arenaPlayRepository.countByStudentId(s.getId()));
        m.put(Metric.ARENA_COMBO, arenaPlayRepository.maxCombo(s.getId()));
        m.put(Metric.PHOTOS, (int) photoProblemRepository.countByStudentId(s.getId()));
        return m;
    }

    private List<Achievement> sorted() {
        return achievementRepository.findAll().stream()
                .sorted(Comparator.comparing(Achievement::getSortOrder).thenComparing(Achievement::getCode))
                .toList();
    }

    private static Metric parse(String metric) {
        try {
            return Metric.valueOf(metric);
        } catch (IllegalArgumentException e) {
            return Metric.STAGES;
        }
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static AchievementView toView(Achievement a, StudentAchievement sa, int value) {
        Integer target = a.getTarget();
        int progress = target == null ? (sa != null ? 1 : 0) : Math.min(value, target);
        return new AchievementView(a.getId(), a.getCode(), a.getName(), a.getDescription(), a.getIconUrl(), a.getPoints(),
                sa != null, sa != null ? sa.getEarnedAt() : null, a.getCriteria(), a.getCategory(),
                a.getTier() == null ? 1 : a.getTier(), sa != null && target != null ? target : progress, target);
    }
}

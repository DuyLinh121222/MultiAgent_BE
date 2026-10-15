package com.mael.service;

import com.mael.dto.learning.*;
import com.mael.entity.*;
import com.mael.entity.enums.ProgressStatus;
import com.mael.entity.enums.ReviewStatus;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lộ trình, chủ đề, bài giảng và tiến độ học. Học sinh chỉ thấy bài giảng đã PUBLISHED.
 */
@Service
@RequiredArgsConstructor
public class LearningService {

    private final LearningPathRepository pathRepository;
    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final LessonMaterialRepository materialRepository;
    private final LessonProgressRepository progressRepository;
    private final WeaknessRepository weaknessRepository;
    private final AchievementService achievementService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<LearningPathSummary> listPaths() {
        return pathRepository.findByActiveTrueOrderByGradeLevelAsc().stream()
                .map(p -> new LearningPathSummary(p.getId(), p.getName(), p.getGradeLevel(),
                        p.getDescription(), p.getTopics().size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningPathDetail pathDetail(String pathId) {
        LearningPath path = pathRepository.findById(pathId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lộ trình"));
        return buildPathDetail(path, currentUser.student());
    }

    /** Lộ trình theo khối lớp của học sinh đang đăng nhập */
    @Transactional(readOnly = true)
    public LearningPathDetail myPath() {
        Student student = currentUser.student();
        LearningPath path = pathRepository.findFirstByGradeLevelAndActiveTrue(student.getGradeLevel())
                .orElseThrow(() -> ApiException.notFound("Chưa có lộ trình cho lớp " + student.getGradeLevel()));
        return buildPathDetail(path, student);
    }

    @Transactional(readOnly = true)
    public LearningPathDetail buildPathDetail(LearningPath path, Student student) {
        Map<String, LessonProgress> progress = progressRepository.findByStudentId(student.getId()).stream()
                .collect(Collectors.toMap(p -> p.getLesson().getId(), Function.identity()));
        Map<String, BigDecimal> mastery = weaknessRepository.findByStudentId(student.getId()).stream()
                .collect(Collectors.toMap(w -> w.getTopic().getId(), Weakness::getMasteryScore));

        List<TopicView> topics = topicRepository.findByLearningPathIdOrderByOrderIndexAsc(path.getId()).stream()
                .map(t -> {
                    List<LessonSummary> lessons = lessonRepository
                            .findByTopicIdAndReviewStatusOrderByOrderIndexAsc(t.getId(), ReviewStatus.PUBLISHED).stream()
                            .map(l -> toSummary(l, progress.get(l.getId())))
                            .toList();
                    int completed = (int) lessons.stream()
                            .filter(l -> l.progressStatus() == ProgressStatus.COMPLETED).count();
                    return new TopicView(t.getId(), t.getName(), t.getDescription(), t.getOrderIndex(),
                            completed, lessons.size(), mastery.get(t.getId()), lessons);
                })
                .toList();

        int total = topics.stream().mapToInt(TopicView::totalLessons).sum();
        int completed = topics.stream().mapToInt(TopicView::completedLessons).sum();
        return new LearningPathDetail(path.getId(), path.getName(), path.getGradeLevel(),
                path.getDescription(), completed, total, topics);
    }

    /**
     * Xem bài giảng. Lần đầu mở sẽ tạo tiến độ IN_PROGRESS.
     */
    @Transactional
    public LessonDetail lessonDetail(String lessonId) {
        Student student = currentUser.student();
        Lesson lesson = getPublishedLesson(lessonId);
        LessonProgress p = progressRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseGet(() -> progressRepository.save(LessonProgress.builder()
                        .student(student).lesson(lesson)
                        .status(ProgressStatus.IN_PROGRESS).progressPercent(0).build()));
        p.setLastAccessedAt(LocalDateTime.now());
        if (p.getStatus() == ProgressStatus.NOT_STARTED) p.setStatus(ProgressStatus.IN_PROGRESS);

        List<MaterialView> materials = materialRepository.findAll().stream()
                .filter(m -> m.getLesson().getId().equals(lessonId))
                .map(m -> new MaterialView(m.getId(), m.getFileName(), m.getFileUrl(), m.getFileType(), m.getFileSize()))
                .toList();
        Topic t = lesson.getTopic();
        return new LessonDetail(lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getVideoUrl(),
                lesson.getDurationMinutes(), t.getId(), t.getName(), t.getLearningPath().getGradeLevel(),
                p.getStatus(), p.getProgressPercent(), materials);
    }

    /**
     * Cập nhật % tiến độ. Đạt 100% thì đánh dấu hoàn thành và xét danh hiệu.
     */
    @Transactional
    public LessonSummary updateProgress(String lessonId, int percent) {
        Student student = currentUser.student();
        Lesson lesson = getPublishedLesson(lessonId);
        LessonProgress p = progressRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder().student(student).lesson(lesson).build());
        if (p.getStatus() != ProgressStatus.COMPLETED) {
            p.setProgressPercent(Math.max(p.getProgressPercent(), percent));
            if (p.getProgressPercent() >= 100) {
                p.setStatus(ProgressStatus.COMPLETED);
                p.setCompletedAt(LocalDateTime.now());
                achievementService.award(student, AchievementService.FIRST_LESSON);
            } else {
                p.setStatus(ProgressStatus.IN_PROGRESS);
            }
        }
        p.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(p);
        return toSummary(lesson, p);
    }

    public Lesson getPublishedLesson(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài học"));
        if (lesson.getReviewStatus() != ReviewStatus.PUBLISHED) {
            throw ApiException.notFound("Bài học chưa được xuất bản");
        }
        return lesson;
    }

    private static LessonSummary toSummary(Lesson l, LessonProgress p) {
        return new LessonSummary(l.getId(), l.getTitle(), l.getDurationMinutes(), l.getOrderIndex(),
                p != null ? p.getStatus() : ProgressStatus.NOT_STARTED,
                p != null ? p.getProgressPercent() : 0);
    }
}

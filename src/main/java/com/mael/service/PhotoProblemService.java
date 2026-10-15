package com.mael.service;

import com.mael.dto.photo.PhotoProblemView;
import com.mael.entity.PhotoProblem;
import com.mael.entity.Solution;
import com.mael.entity.Student;
import com.mael.entity.Topic;
import com.mael.entity.enums.PhotoStatus;
import com.mael.exception.ApiException;
import com.mael.repository.LearningPathRepository;
import com.mael.repository.PhotoProblemRepository;
import com.mael.repository.SolutionRepository;
import com.mael.repository.TopicRepository;
import com.mael.security.CurrentUser;
import com.mael.service.photo.PhotoSolver;
import com.mael.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

/**
 * Giải toán bằng ảnh: học sinh chụp/tải ảnh bài toán, AI đọc đề và giải từng bước.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoProblemService {

    private final PhotoProblemRepository photoProblemRepository;
    private final SolutionRepository solutionRepository;
    private final TopicRepository topicRepository;
    private final LearningPathRepository pathRepository;
    private final FileStorageService fileStorageService;
    private final PhotoSolver photoSolver;
    private final SubscriptionService subscriptionService;
    private final AchievementService achievementService;
    private final CurrentUser currentUser;

    @Transactional
    public PhotoProblemView upload(MultipartFile image, String note) {
        Student student = currentUser.student();
        subscriptionService.requireActive(student);

        FileStorageService.StoredFile stored = fileStorageService.storeImage(image, "photo-problems");
        PhotoProblem problem = photoProblemRepository.save(PhotoProblem.builder()
                .student(student)
                .imageUrl(stored.url())
                .extractedText(note)
                .status(PhotoStatus.PENDING)
                .build());

        try {
            PhotoSolver.SolveResult result = photoSolver.solve(stored.path(), note, student.getGradeLevel());
            problem.setExtractedText(result.extractedText());
            problem.setTopic(matchTopic(result.topicHint(), student.getGradeLevel()));
            Solution solution = solutionRepository.save(Solution.builder()
                    .photoProblem(problem)
                    .finalAnswer(result.finalAnswer())
                    .steps(String.join("\n", result.steps()))
                    .generatedBy(result.modelName())
                    .build());
            problem.setSolution(solution);
            problem.setStatus(PhotoStatus.SOLVED);
        } catch (Exception e) {
            log.warn("Giải ảnh thất bại: {}", e.getMessage());
            problem.setStatus(PhotoStatus.FAILED);
        }
        achievementService.award(student, AchievementService.PHOTO_SOLVER);
        return DtoMapper.toPhotoView(problem);
    }

    @Transactional(readOnly = true)
    public List<PhotoProblemView> myProblems() {
        return photoProblemRepository.findByStudentId(currentUser.student().getId()).stream()
                .map(DtoMapper::toPhotoView).toList();
    }

    @Transactional(readOnly = true)
    public PhotoProblemView get(String id) {
        String studentId = currentUser.student().getId();
        return photoProblemRepository.findById(id)
                .filter(p -> p.getStudent().getId().equals(studentId))
                .map(DtoMapper::toPhotoView)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài toán"));
    }

    /** Tìm chủ đề trong lộ trình của khối lớp có tên chứa topicHint */
    private Topic matchTopic(String hint, int gradeLevel) {
        if (hint == null || hint.isBlank()) return null;
        String h = hint.toLowerCase(Locale.ROOT);
        return pathRepository.findFirstByGradeLevelAndActiveTrue(gradeLevel)
                .flatMap(p -> topicRepository.findByLearningPathIdOrderByOrderIndexAsc(p.getId()).stream()
                        .filter(t -> t.getName().toLowerCase(Locale.ROOT).contains(h))
                        .findFirst())
                .orElse(null);
    }
}

package com.mael.service;

import com.mael.dto.quiz.*;
import com.mael.entity.*;
import com.mael.entity.enums.*;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quiz luyện tập / củng cố. Chấm điểm bằng code và cập nhật mức nắm vững (WEAKNESS) theo chủ đề.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository attemptRepository;
    private final WeaknessRepository weaknessRepository;
    private final AchievementService achievementService;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<QuizSummary> availableQuizzes() {
        String studentId = currentUser.student().getId();
        Map<String, BigDecimal> best = attemptRepository.findByStudentId(studentId).stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED && a.getScore() != null)
                .collect(Collectors.toMap(a -> a.getQuiz().getId(), QuizAttempt::getScore, BigDecimal::max));
        return quizRepository.findAvailableForStudent(studentId).stream()
                .map(q -> new QuizSummary(q.getId(), q.getTitle(), q.getQuizType(), q.getTimeLimitMinutes(),
                        q.getQuestions().size(),
                        q.getWeakness() != null ? q.getWeakness().getTopic().getName() : null,
                        best.get(q.getId())))
                .toList();
    }

    @Transactional
    public StartAttemptResponse startAttempt(String quizId) {
        Student student = currentUser.student();
        Quiz quiz = getAccessibleQuiz(quizId, student);
        QuizAttempt attempt = attemptRepository.save(QuizAttempt.builder()
                .student(student).quiz(quiz).status(AttemptStatus.IN_PROGRESS).build());
        return new StartAttemptResponse(attempt.getId(), attempt.getStartedAt(), toDetail(quiz));
    }

    @Transactional
    public AttemptResult submit(String attemptId, SubmitAttemptRequest req) {
        Student student = currentUser.student();
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .filter(a -> a.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy lượt làm bài"));
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw ApiException.badRequest("Bài này đã nộp rồi");
        }
        Quiz quiz = quizRepository.findWithQuestions(attempt.getQuiz().getId()).orElseThrow();
        Map<String, String> answers = new HashMap<>();
        req.answers().forEach(a -> answers.put(a.quizQuestionId(), a.selectedAnswer()));

        BigDecimal score = BigDecimal.ZERO;
        BigDecimal max = BigDecimal.ZERO;
        int correctCount = 0;
        List<AttemptResult.AnswerResult> results = new ArrayList<>();
        // Thống kê đúng/tổng theo chủ đề để cập nhật điểm yếu
        Map<Topic, int[]> byTopic = new LinkedHashMap<>();

        for (QuizQuestion qq : quiz.getQuestions()) {
            Question q = qq.getQuestion();
            String selected = answers.get(qq.getId());
            boolean correct = isCorrect(q, selected);
            BigDecimal earned = correct ? qq.getPoints() : BigDecimal.ZERO;
            max = max.add(qq.getPoints());
            score = score.add(earned);
            if (correct) correctCount++;

            attempt.getAnswers().add(AttemptAnswer.builder()
                    .attempt(attempt).quizQuestion(qq)
                    .selectedAnswer(selected).correct(correct).earnedPoints(earned)
                    .build());
            results.add(new AttemptResult.AnswerResult(qq.getId(), q.getContent(), selected,
                    q.getCorrectAnswer(), correct, q.getExplanation()));

            int[] stat = byTopic.computeIfAbsent(q.getTopic(), t -> new int[2]);
            stat[1]++;
            if (correct) stat[0]++;
        }

        attempt.setScore(score);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setStatus(AttemptStatus.SUBMITTED);

        Weakness mainWeakness = null;
        for (var e : byTopic.entrySet()) {
            Weakness w = updateWeakness(student, e.getKey(), e.getValue()[0], e.getValue()[1]);
            if (mainWeakness == null) mainWeakness = w;
        }

        boolean unlocked = false;
        if (max.signum() > 0 && score.compareTo(max) == 0) {
            unlocked = achievementService.award(student, AchievementService.QUIZ_MASTER);
        }
        notificationService.notify(student, NotificationType.QUIZ, "Đã chấm bài: " + quiz.getTitle(),
                "Bạn đúng " + correctCount + "/" + quiz.getQuestions().size() + " câu.", null);

        return new AttemptResult(attempt.getId(), quiz.getTitle(), score, max, correctCount,
                quiz.getQuestions().size(), results, DtoMapper.toWeaknessView(mainWeakness), unlocked);
    }

    @Transactional(readOnly = true)
    public List<WeaknessView> myWeaknesses() {
        return weaknessRepository.findByStudentId(currentUser.student().getId()).stream()
                .map(DtoMapper::toWeaknessView).toList();
    }

    // ------------------------------------------------------------------

    private Quiz getAccessibleQuiz(String quizId, Student student) {
        Quiz quiz = quizRepository.findWithQuestions(quizId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài quiz"));
        if (quiz.getWeakness() != null && !quiz.getWeakness().getStudent().getId().equals(student.getId())) {
            throw ApiException.notFound("Không tìm thấy bài quiz");
        }
        if (quiz.getQuestions().isEmpty()) {
            throw ApiException.badRequest("Bài quiz chưa có câu hỏi");
        }
        return quiz;
    }

    private QuizDetail toDetail(Quiz quiz) {
        List<QuizQuestionView> questions = quiz.getQuestions().stream()
                .map(qq -> new QuizQuestionView(qq.getId(), qq.getOrderIndex(), qq.getQuestion().getContent(),
                        qq.getQuestion().getQuestionType(), qq.getQuestion().getOptions(), qq.getPoints()))
                .toList();
        return new QuizDetail(quiz.getId(), quiz.getTitle(), quiz.getQuizType(), quiz.getTimeLimitMinutes(), questions);
    }

    /**
     * Trắc nghiệm: so chữ cái đáp án (A/B/C/D). Tự luận ngắn: so chuỗi sau khi bỏ khoảng trắng, không phân biệt hoa thường.
     */
    static boolean isCorrect(Question q, String selected) {
        if (selected == null || selected.isBlank()) return false;
        String expected = normalize(q.getCorrectAnswer());
        String actual = normalize(selected);
        if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
            return expected.equals(actual);
        }
        if (q.getQuestionType() == QuestionType.MULTI_CHOICE) {
            return sortedLetters(expected).equals(sortedLetters(actual));
        }
        // "B" hoặc "B. x = 5 hoặc x = -2" đều được
        return actual.equals(expected) || actual.startsWith(expected + ".");
    }

    private static String normalize(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String sortedLetters(String s) {
        return Arrays.stream(s.split("[,;]")).map(String::trim).sorted().collect(Collectors.joining(","));
    }

    /**
     * Cập nhật mức nắm vững: điểm mới = 50% điểm cũ + 50% điểm lần làm này.
     * Chưa có điểm yếu mà làm dưới 80% thì tạo mới.
     */
    private Weakness updateWeakness(Student student, Topic topic, int correct, int total) {
        BigDecimal pct = BigDecimal.valueOf(correct * 100.0 / total).setScale(2, RoundingMode.HALF_UP);
        Optional<Weakness> existing = weaknessRepository.findByStudentIdAndTopicId(student.getId(), topic.getId());
        if (existing.isEmpty() && pct.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return null;
        }
        Weakness w = existing.orElseGet(() -> Weakness.builder().student(student).topic(topic).masteryScore(pct).build());
        BigDecimal old = w.getMasteryScore();
        BigDecimal mastery = existing.isPresent()
                ? old.add(pct).divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                : pct;
        w.setMasteryScore(mastery.min(HUNDRED));
        w.setSeverity(mastery.compareTo(BigDecimal.valueOf(50)) < 0 ? Severity.HIGH
                : mastery.compareTo(BigDecimal.valueOf(75)) < 0 ? Severity.MEDIUM : Severity.LOW);
        if (mastery.compareTo(BigDecimal.valueOf(85)) >= 0) {
            w.setStatus(WeaknessStatus.RESOLVED);
        } else if (existing.isPresent() && mastery.compareTo(old) > 0) {
            w.setStatus(WeaknessStatus.IMPROVING);
        } else {
            w.setStatus(WeaknessStatus.OPEN);
        }
        return weaknessRepository.save(w);
    }
}

package com.mael.service;

import com.mael.dto.game.*;
import com.mael.entity.*;
import com.mael.entity.enums.NotificationType;
import com.mael.entity.enums.PetSource;
import com.mael.entity.enums.ProgressStatus;
import com.mael.entity.enums.ReviewStatus;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bản đồ ải: mỗi bài học là 1 ải (3 câu thử thách), cuối mỗi chương có 1 ải trùm (5 câu ngẫu nhiên trong chương).
 *
 * Luật chơi:
 * - Ải mở lần lượt: qua ải trước (>= 1 sao) mới mở ải sau. Ải trùm mở khi qua hết các ải trong chương.
 * - Sao: đúng 100% = 3 sao, >= 2/3 = 2 sao, >= 1/3 = 1 sao, thấp hơn = chưa qua.
 * - Xu: 10 xu cho mỗi sao MỚI đạt được (chơi lại để tăng sao vẫn được xu phần chênh lệch).
 * - Kinh nghiệm: 5 xp mỗi câu đúng, +20 xp khi qua ải lần đầu. 100 xp lên 1 cấp.
 * - Rương báu ải trùm (lần đầu phá đảo): thú của chương + 50 xu.
 */
@Service
@RequiredArgsConstructor
public class GameService {

    public static final int COINS_PER_STAR = 10;
    public static final int XP_PER_CORRECT = 5;
    public static final int XP_FIRST_CLEAR = 20;
    public static final int XP_PER_LEVEL = 100;
    public static final int BOSS_QUESTIONS = 5;
    public static final int BOSS_CHEST_COINS = 50;
    public static final int WELCOME_COINS = 30;

    private static final String LESSON = "LESSON";
    private static final String BOSS = "BOSS";

    private final CurrentUser currentUser;
    private final StudentRepository studentRepository;
    private final LearningPathRepository pathRepository;
    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final StageResultRepository stageResultRepository;
    private final LessonProgressRepository progressRepository;
    private final PetRepository petRepository;
    private final StudentPetRepository studentPetRepository;
    private final AchievementService achievementService;
    private final NotificationService notificationService;
    private final QuizService quizService;
    private final com.mael.service.game.StaminaService staminaService;
    private final ArenaPlayRepository arenaPlayRepository;

    public static final int ARENA_QUESTIONS = 10;
    public static final int ARENA_TIME_SECONDS = 120;
    public static final int ARENA_HEARTS = 3;
    public static final int ARENA_COINS_PER_CORRECT = 2;
    public static final int ARENA_DAILY_COIN_CAP = 40;

    // =================================================================
    //  Bản đồ
    // =================================================================

    @Transactional(readOnly = true)
    public MapResponse myMap() {
        Student student = currentUser.student();
        GameMap map = buildMap(student);
        int cleared = 0;
        int total = 0;
        for (ChapterNode c : map.chapters) {
            for (StageNode s : c.stages()) {
                total++;
                if ("CLEARED".equals(s.status())) cleared++;
            }
            if (c.boss() != null) {
                total++;
                if ("CLEARED".equals(c.boss().status())) cleared++;
            }
        }
        return new MapResponse(profile(student), student.getGradeLevel(),
                map.path != null ? map.path.getName() : "Toán lớp " + student.getGradeLevel(),
                map.path != null ? map.path.getDescription() : null,
                cleared, total,
                map.next != null ? map.next.type : null,
                map.next != null ? map.next.id : null,
                map.next != null ? map.next.title : null,
                map.chapters);
    }

    @Transactional(readOnly = true)
    public GameProfile myProfile() {
        return profile(currentUser.student());
    }

    // =================================================================
    //  Ải thường (bài học)
    // =================================================================

    @Transactional
    public StageDetail lessonStage(String lessonId) {
        Student student = currentUser.student();
        GameMap map = buildMap(student);
        Slot slot = map.require(LESSON, lessonId);
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();

        // Mở ải lần đầu = bắt đầu học bài
        LessonProgress p = progressRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseGet(() -> LessonProgress.builder().student(student).lesson(lesson)
                        .status(ProgressStatus.IN_PROGRESS).progressPercent(0).build());
        if (p.getStatus() == ProgressStatus.NOT_STARTED) p.setStatus(ProgressStatus.IN_PROGRESS);
        p.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(p);

        List<StageQuestion> questions = lessonQuestions(lessonId).stream().map(GameService::toStageQuestion).toList();
        return new StageDetail(LESSON, lessonId, slot.levelNumber, lesson.getTitle(), lesson.getTopic().getName(),
                lesson.getContent(), lesson.getDurationMinutes(),
                lesson.getMisconceptions() == null ? List.of() : lesson.getMisconceptions(),
                slot.stars, questions, null, null);
    }

    @Transactional
    public StageResultView submitLesson(String lessonId, SubmitStageRequest req) {
        Student student = currentUser.student();
        GameMap map = buildMap(student);
        map.require(LESSON, lessonId);
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        List<Question> questions = lessonQuestions(lessonId);
        // Mỗi lần nộp thử thách tốn 1 thể lực (ải chưa có câu hỏi thì không tốn)
        if (!questions.isEmpty()) staminaService.consume(student);

        Grade grade = grade(questions, req.answers());
        // Ải chưa có câu thử thách: học xong lý thuyết là qua ải
        int stars = questions.isEmpty() ? 3 : starsFor(grade.correct, grade.total);

        StageResult result = stageResultRepository.findByStudentIdAndLessonId(student.getId(), lessonId)
                .orElseGet(() -> StageResult.builder().student(student).lesson(lesson).build());
        Reward reward = applyResult(student, result, stars, grade);

        if (stars > 0) {
            completeLesson(student, lesson);
        }
        if (!questions.isEmpty()) {
            quizService.updateWeakness(student, lesson.getTopic(), grade.correct, grade.total);
        }
        return buildResultView(student, LESSON, lessonId, grade, stars, reward, null);
    }

    // =================================================================
    //  Ải trùm cuối chương
    // =================================================================

    @Transactional(readOnly = true)
    public StageDetail bossStage(String topicId) {
        Student student = currentUser.student();
        GameMap map = buildMap(student);
        Slot slot = map.require(BOSS, topicId);
        Topic topic = topicRepository.findById(topicId).orElseThrow();

        List<Question> pool = new ArrayList<>(questionRepository.findByTopicIdAndReviewStatus(topicId, ReviewStatus.PUBLISHED));
        Collections.shuffle(pool);
        List<StageQuestion> questions = pool.stream().limit(BOSS_QUESTIONS).map(GameService::toStageQuestion).toList();
        Pet pet = topic.getRewardPetCode() == null ? null : petRepository.findById(topic.getRewardPetCode()).orElse(null);
        return new StageDetail(BOSS, topicId, slot.levelNumber, "Ải trùm: " + topic.getName(), topic.getName(),
                null, null, List.of(), slot.stars, questions,
                pet != null ? pet.getCode() : null, pet != null ? pet.getName() : null);
    }

    @Transactional
    public StageResultView submitBoss(String topicId, SubmitStageRequest req) {
        Student student = currentUser.student();
        GameMap map = buildMap(student);
        map.require(BOSS, topicId);
        Topic topic = topicRepository.findById(topicId).orElseThrow();
        staminaService.consume(student);

        // Chỉ chấm các câu thuộc đúng chương này (đề ải trùm được bốc ngẫu nhiên ở FE)
        Set<String> ids = req.answers().keySet();
        List<Question> questions = questionRepository.findAllById(ids).stream()
                .filter(q -> q.getTopic().getId().equals(topicId) && q.getReviewStatus() == ReviewStatus.PUBLISHED)
                .toList();
        int expected = (int) Math.min(BOSS_QUESTIONS,
                questionRepository.findByTopicIdAndReviewStatus(topicId, ReviewStatus.PUBLISHED).size());
        if (questions.size() < expected) {
            throw ApiException.badRequest("Bài làm ải trùm chưa đủ " + expected + " câu");
        }

        Grade grade = grade(questions, req.answers());
        int stars = starsFor(grade.correct, grade.total);
        StageResult result = stageResultRepository.findByStudentIdAndTopicId(student.getId(), topicId)
                .orElseGet(() -> StageResult.builder().student(student).topic(topic).build());
        Reward reward = applyResult(student, result, stars, grade);

        PetView rewardPet = null;
        if (reward.firstClear) {
            // Mở rương báu
            student.setCoins(student.getCoins() + BOSS_CHEST_COINS);
            reward.coins += BOSS_CHEST_COINS;
            Pet pet = topic.getRewardPetCode() == null ? null : petRepository.findById(topic.getRewardPetCode()).orElse(null);
            if (pet != null) {
                givePet(student, pet, PetSource.CHEST);
                rewardPet = toPetView(pet, true, pet.getCode().equals(student.getActivePetCode()), topicNameByPet());
            }
            notificationService.notify(student, NotificationType.ACHIEVEMENT,
                    "Phá đảo " + topic.getName() + "!",
                    "Bạn mở rương báu nhận " + BOSS_CHEST_COINS + " xu" + (pet != null ? " và thú " + pet.getName() : "") + ".",
                    null);
        }
        quizService.updateWeakness(student, topic, grade.correct, grade.total);
        return buildResultView(student, BOSS, topicId, grade, stars, reward, rewardPet);
    }

    // =================================================================
    //  Đấu trường (mini game giải bài nhanh)
    // =================================================================

    @Transactional(readOnly = true)
    public List<ArenaSummary> arenas() {
        Student student = currentUser.student();
        GameMap map = buildMap(student);
        List<ArenaSummary> list = new ArrayList<>();
        for (ChapterNode c : map.chapters) {
            int count = arenaPool(c.topicId()).size();
            if (count == 0) continue;
            Topic t = topicRepository.findById(c.topicId()).orElseThrow();
            list.add(new ArenaSummary(c.topicId(), c.orderIndex(), c.name(), c.unlocked(), count,
                    arenaPlayRepository.bestScore(student.getId(), c.topicId()), t.getRewardPetCode()));
        }
        return list;
    }

    /** Lượt chơi đang diễn ra (lưu tạm trong bộ nhớ, hết hạn sau 15 phút) */
    private static final class ArenaRun {
        final String studentId;
        final String topicId;
        final List<String> questionIds;
        final Map<String, String> answers = new LinkedHashMap<>();
        final long createdAt = System.currentTimeMillis();

        ArenaRun(String studentId, String topicId, List<String> questionIds) {
            this.studentId = studentId;
            this.topicId = topicId;
            this.questionIds = questionIds;
        }
    }

    private final Map<String, ArenaRun> arenaRuns = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Bắt đầu 1 lượt Đấu trường: trừ 1 thể lực, bốc tối đa 10 câu (ưu tiên câu ngắn).
     * Đáp án được chấm từng câu ở BE (FE không biết trước đáp án).
     */
    @Transactional
    public ArenaDetail startArena(String topicId) {
        Student student = currentUser.student();
        Topic topic = requireArenaUnlocked(student, topicId);
        List<Question> pool = new ArrayList<>(arenaPool(topicId));
        if (pool.isEmpty()) throw ApiException.badRequest("Đấu trường này chưa có câu hỏi");
        staminaService.consume(student);
        studentRepository.save(student);
        Collections.shuffle(pool);
        pool.sort(Comparator.comparingInt(q -> q.getContent().length() > 90 ? 1 : 0));
        List<Question> picked = pool.stream().limit(ARENA_QUESTIONS).toList();
        long now = System.currentTimeMillis();
        arenaRuns.values().removeIf(r -> now - r.createdAt > 15 * 60_000L);
        String runId = UUID.randomUUID().toString();
        arenaRuns.put(runId, new ArenaRun(student.getId(), topicId, picked.stream().map(Question::getId).toList()));
        return new ArenaDetail(runId, topicId, topic.getName(), ARENA_TIME_SECONDS, ARENA_HEARTS,
                arenaPlayRepository.bestScore(student.getId(), topicId),
                picked.stream().map(GameService::toStageQuestion).toList(), profile(student));
    }

    /** Chấm 1 câu ngay khi học sinh bấm bóng đáp án (mỗi câu chỉ tính lần chọn đầu tiên) */
    @Transactional(readOnly = true)
    public ArenaAnswerView answerArena(String runId, ArenaAnswerRequest req) {
        ArenaRun run = ownRun(runId);
        if (!run.questionIds.contains(req.questionId())) throw ApiException.badRequest("Câu hỏi không thuộc lượt chơi này");
        Question q = questionRepository.findById(req.questionId()).orElseThrow();
        String first = run.answers.putIfAbsent(req.questionId(), req.answer());
        String used = first != null ? first : req.answer();
        boolean ok = QuizService.isCorrect(q, used);
        return new ArenaAnswerView(ok, q.getCorrectAnswer(), q.getExplanation());
    }

    @Transactional
    public ArenaResultView finishArena(String runId, ArenaFinishRequest req) {
        ArenaRun run = ownRun(runId);
        arenaRuns.remove(runId);
        Student student = currentUser.student();
        Topic topic = topicRepository.findById(run.topicId).orElseThrow();
        List<Question> questions = questionRepository.findAllById(run.answers.keySet());
        Grade grade = grade(questions, run.answers);
        // Combo tính lại ở BE theo đúng thứ tự trả lời
        int combo = 0, cur = 0;
        Map<String, Question> byId = new HashMap<>();
        questions.forEach(q -> byId.put(q.getId(), q));
        for (Map.Entry<String, String> e : run.answers.entrySet()) {
            if (QuizService.isCorrect(byId.get(e.getKey()), e.getValue())) combo = Math.max(combo, ++cur);
            else cur = 0;
        }
        int timeLeft = Math.max(0, Math.min(req == null || req.timeLeft() == null ? 0 : req.timeLeft(), ARENA_TIME_SECONDS));
        if (System.currentTimeMillis() - run.createdAt > (ARENA_TIME_SECONDS + 30) * 1000L) timeLeft = 0;
        int score = grade.correct * 100 + combo * 30 + (grade.correct == run.questionIds.size() ? timeLeft * 2 : 0);
        int best = arenaPlayRepository.bestScore(student.getId(), run.topicId);

        // Xu có giới hạn mỗi ngày để không cày lấy xu, XP vẫn tính
        LocalDateTime startOfDay = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).atStartOfDay();
        int coinsToday = arenaPlayRepository.coinsSince(student.getId(), startOfDay);
        int coins = Math.max(0, Math.min(grade.correct * ARENA_COINS_PER_CORRECT, ARENA_DAILY_COIN_CAP - coinsToday));
        int xp = grade.correct * 2;
        int oldLevel = student.getXp() / XP_PER_LEVEL;
        student.setCoins(student.getCoins() + coins);
        student.setXp(student.getXp() + xp);

        arenaPlayRepository.save(ArenaPlay.builder().student(student).topic(topic)
                .correct(grade.correct).total(grade.total).bestCombo(combo).score(score).coinsEarned(coins).build());
        if (grade.total > 0) quizService.updateWeakness(student, topic, grade.correct, grade.total);

        var streak = staminaService.recordActivity(student);
        studentRepository.save(student);
        var newAchievements = achievementService.evaluate(student);
        studentRepository.save(student);
        return new ArenaResultView(grade.correct, grade.total, combo, score, Math.max(best, score), score > best,
                coins, xp, coinsToday + coins >= ARENA_DAILY_COIN_CAP, student.getXp() / XP_PER_LEVEL > oldLevel,
                grade.feedback, newAchievements, streak.streakDays(), streak.increased(), profile(student));
    }

    private ArenaRun ownRun(String runId) {
        ArenaRun run = arenaRuns.get(runId);
        if (run == null || !run.studentId.equals(currentUser.student().getId())) {
            throw ApiException.notFound("Lượt chơi đã hết hạn, vào lại Đấu trường nhé");
        }
        return run;
    }

    private List<Question> arenaPool(String topicId) {
        return questionRepository.findByTopicIdAndReviewStatus(topicId, ReviewStatus.PUBLISHED).stream()
                .filter(q -> q.getQuestionType() == com.mael.entity.enums.QuestionType.SINGLE_CHOICE
                        && q.getOptions() != null && q.getOptions().size() >= 2)
                .toList();
    }

    private Topic requireArenaUnlocked(Student student, String topicId) {
        GameMap map = buildMap(student);
        ChapterNode c = map.chapters.stream().filter(x -> x.topicId().equals(topicId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đấu trường trong lộ trình của bạn"));
        if (!c.unlocked()) throw ApiException.forbidden("Đấu trường này mở khi em tới được chương " + c.orderIndex() + " trên bản đồ");
        return topicRepository.findById(topicId).orElseThrow();
    }

    // =================================================================
    //  Thú đồng hành & màn chào mừng
    // =================================================================

    @Transactional(readOnly = true)
    public List<PetView> myPets() {
        Student student = currentUser.student();
        Set<String> owned = studentPetRepository.findByStudentId(student.getId()).stream()
                .map(sp -> sp.getPet().getCode()).collect(Collectors.toSet());
        Map<String, String> chapterByPet = topicNameByPet();
        return petRepository.findAllByOrderBySortOrderAsc().stream()
                .map(p -> toPetView(p, owned.contains(p.getCode()), p.getCode().equals(student.getActivePetCode()), chapterByPet))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PetView> starterPets() {
        return petRepository.findAllByOrderBySortOrderAsc().stream()
                .filter(Pet::getStarter)
                .map(p -> toPetView(p, false, false, Map.of()))
                .toList();
    }

    @Transactional
    public List<PetView> buyPet(String petCode) {
        Student student = currentUser.student();
        Pet pet = petRepository.findById(petCode).orElseThrow(() -> ApiException.notFound("Không tìm thấy thú"));
        if (pet.getPriceCoins() == null) {
            throw ApiException.badRequest(pet.getName() + " chỉ có trong rương báu ải trùm, không bán bằng xu");
        }
        if (studentPetRepository.existsByStudentIdAndPetCode(student.getId(), petCode)) {
            throw ApiException.conflict("Bạn đã có " + pet.getName() + " rồi");
        }
        if (student.getCoins() < pet.getPriceCoins()) {
            throw ApiException.badRequest("Bạn còn thiếu " + (pet.getPriceCoins() - student.getCoins()) + " xu. Qua thêm ải để kiếm xu nhé!");
        }
        student.setCoins(student.getCoins() - pet.getPriceCoins());
        givePet(student, pet, PetSource.SHOP);
        student.setActivePetCode(petCode);
        achievementService.evaluate(student);
        return myPets();
    }

    /** Mua 1 khiên giữ chuỗi bằng xu */
    @Transactional
    public GameProfile buyStreakShield() {
        Student student = currentUser.student();
        staminaService.buyShield(student);
        studentRepository.save(student);
        return profile(student);
    }

    @Transactional
    public GameProfile setActivePet(String petCode) {
        Student student = currentUser.student();
        if (!studentPetRepository.existsByStudentIdAndPetCode(student.getId(), petCode)) {
            throw ApiException.badRequest("Bạn chưa sở hữu thú này");
        }
        student.setActivePetCode(petCode);
        return profile(student);
    }

    /**
     * Hoàn tất màn chào mừng: lưu lớp, mục tiêu, tặng thú khởi đầu + 30 xu.
     */
    @Transactional
    public GameProfile completeOnboarding(OnboardingRequest req) {
        Student student = currentUser.student();
        Pet pet = petRepository.findById(req.petCode())
                .filter(Pet::getStarter)
                .orElseThrow(() -> ApiException.badRequest("Hãy chọn 1 trong 3 thú khởi đầu"));
        student.setGradeLevel(req.gradeLevel());
        student.setLearningGoal(req.goal());
        if (!Boolean.TRUE.equals(student.getOnboarded())) {
            student.setCoins(student.getCoins() + WELCOME_COINS);
        }
        student.setOnboarded(true);
        givePet(student, pet, PetSource.STARTER);
        student.setActivePetCode(pet.getCode());
        studentRepository.save(student);
        return profile(student);
    }

    // =================================================================
    //  Nội bộ
    // =================================================================

    public GameProfile profile(Student s) {
        int xp = s.getXp() == null ? 0 : s.getXp();
        int totalStars = stageResultRepository.findByStudentId(s.getId()).stream().mapToInt(StageResult::getStars).sum();
        String petName = s.getActivePetCode() == null ? null
                : petRepository.findById(s.getActivePetCode()).map(Pet::getName).orElse(null);
        var energy = staminaService.refresh(s);
        var streak = staminaService.streak(s);
        return new GameProfile(s.getFullName(), s.getGradeLevel(), Boolean.TRUE.equals(s.getOnboarded()),
                s.getLearningGoal() != null ? s.getLearningGoal().name() : null,
                s.getCoins() == null ? 0 : s.getCoins(), xp, xp / XP_PER_LEVEL + 1, xp % XP_PER_LEVEL, XP_PER_LEVEL,
                totalStars, s.getActivePetCode(), petName,
                energy.energy(), energy.maxEnergy(), energy.nextInSeconds(),
                streak.streakDays(), streak.bestStreak(), streak.playedToday(), streak.atRisk(), streak.shields());
    }

    static int starsFor(int correct, int total) {
        if (total <= 0) return 0;
        if (correct >= total) return 3;
        if (correct * 3 >= total * 2) return 2;
        if (correct * 3 >= total) return 1;
        return 0;
    }

    private Reward applyResult(Student student, StageResult result, int stars, Grade grade) {
        int oldStars = result.getStars() == null ? 0 : result.getStars();
        int oldLevel = student.getXp() / XP_PER_LEVEL;
        Reward reward = new Reward();
        reward.previousStars = oldStars;

        result.setAttempts(result.getAttempts() + 1);
        result.setTotalQuestions(grade.total);
        result.setBestCorrect(Math.max(result.getBestCorrect(), grade.correct));
        if (stars > oldStars) {
            reward.coins = (stars - oldStars) * COINS_PER_STAR;
            result.setStars(stars);
        }
        reward.xp = grade.correct * XP_PER_CORRECT;
        if (stars > 0 && result.getFirstClearedAt() == null) {
            result.setFirstClearedAt(LocalDateTime.now());
            reward.firstClear = true;
            reward.xp += XP_FIRST_CLEAR;
        }
        stageResultRepository.save(result);

        student.setCoins(student.getCoins() + reward.coins);
        student.setXp(student.getXp() + reward.xp);
        reward.levelUp = student.getXp() / XP_PER_LEVEL > oldLevel;
        return reward;
    }

    private StageResultView buildResultView(Student student, String type, String id, Grade grade, int stars,
                                            Reward reward, PetView rewardPet) {
        var streak = staminaService.recordActivity(student);
        studentRepository.save(student);
        var newAchievements = achievementService.evaluate(student);
        studentRepository.save(student);
        // Tính lại bản đồ sau khi lưu kết quả để biết ải tiếp theo
        GameMap after = buildMap(student);
        Slot next = after.next;
        return new StageResultView(type, grade.correct, grade.total, stars, reward.previousStars, stars > 0,
                reward.firstClear, reward.coins, reward.xp, reward.levelUp, rewardPet, grade.feedback,
                next != null ? next.type : null, next != null ? next.id : null, next != null ? next.title : null,
                profile(student), newAchievements, streak.streakDays(), streak.increased());
    }

    private void completeLesson(Student student, Lesson lesson) {
        LessonProgress p = progressRepository.findByStudentIdAndLessonId(student.getId(), lesson.getId())
                .orElseGet(() -> LessonProgress.builder().student(student).lesson(lesson).build());
        if (p.getStatus() != ProgressStatus.COMPLETED) {
            p.setStatus(ProgressStatus.COMPLETED);
            p.setProgressPercent(100);
            p.setCompletedAt(LocalDateTime.now());
            achievementService.award(student, AchievementService.FIRST_LESSON);
        }
        p.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(p);
    }

    private void givePet(Student student, Pet pet, PetSource source) {
        if (!studentPetRepository.existsByStudentIdAndPetCode(student.getId(), pet.getCode())) {
            studentPetRepository.save(StudentPet.builder().student(student).pet(pet).source(source).build());
        }
    }

    private List<Question> lessonQuestions(String lessonId) {
        return questionRepository.findByLessonIdAndReviewStatusOrderByCreatedAtAsc(lessonId, ReviewStatus.PUBLISHED);
    }

    private static Grade grade(List<Question> questions, Map<String, String> answers) {
        Grade g = new Grade();
        for (Question q : questions) {
            String selected = answers.get(q.getId());
            boolean ok = QuizService.isCorrect(q, selected);
            g.total++;
            if (ok) g.correct++;
            g.feedback.add(new AnswerFeedback(q.getId(), q.getContent(), selected, q.getCorrectAnswer(), ok, q.getExplanation()));
        }
        return g;
    }

    private static StageQuestion toStageQuestion(Question q) {
        return new StageQuestion(q.getId(), q.getContent(), q.getOptions());
    }

    private Map<String, String> topicNameByPet() {
        return topicRepository.findAll().stream()
                .filter(t -> t.getRewardPetCode() != null)
                .collect(Collectors.toMap(Topic::getRewardPetCode, Topic::getName, (a, b) -> a));
    }

    private static PetView toPetView(Pet p, boolean owned, boolean active, Map<String, String> chapterByPet) {
        String how;
        if (p.getStarter()) how = "Chọn miễn phí khi bắt đầu" + (p.getPriceCoins() != null ? ", hoặc đổi " + p.getPriceCoins() + " xu" : "");
        else if (p.getPriceCoins() != null) how = "Đổi " + p.getPriceCoins() + " xu";
        else if (chapterByPet.containsKey(p.getCode())) how = "Rương báu khi phá đảo " + chapterByPet.get(p.getCode());
        else how = "Rương báu ải trùm";
        return new PetView(p.getCode(), p.getName(), p.getRarity(), p.getDescription(), p.getPriceCoins(),
                p.getStarter(), owned, active, how);
    }

    /**
     * Dựng bản đồ: duyệt các chương theo thứ tự, ải nào mở / đã qua, ải tiếp theo là gì.
     */
    private GameMap buildMap(Student student) {
        GameMap map = new GameMap();
        map.path = pathRepository.findFirstByGradeLevelAndActiveTrue(student.getGradeLevel()).orElse(null);
        if (map.path == null) return map;

        Map<String, StageResult> byLesson = new HashMap<>();
        Map<String, StageResult> byTopic = new HashMap<>();
        for (StageResult r : stageResultRepository.findByStudentId(student.getId())) {
            if (r.getLesson() != null) byLesson.put(r.getLesson().getId(), r);
            if (r.getTopic() != null) byTopic.put(r.getTopic().getId(), r);
        }
        List<Lesson> lessons = lessonRepository.findByPathAndStatus(map.path.getId(), ReviewStatus.PUBLISHED);
        Map<String, List<Lesson>> lessonsByTopic = lessons.stream()
                .collect(Collectors.groupingBy(l -> l.getTopic().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<String, Long> questionCount = new HashMap<>();
        Map<String, Long> topicQuestionCount = new HashMap<>();
        for (Lesson l : lessons) {
            questionCount.put(l.getId(), (long) lessonQuestions(l.getId()).size());
        }
        Map<String, Pet> pets = petRepository.findAll().stream().collect(Collectors.toMap(Pet::getCode, Function.identity()));

        boolean previousCleared = true; // ải đầu tiên luôn mở
        int level = 0;
        for (Topic t : topicRepository.findByLearningPathIdOrderByOrderIndexAsc(map.path.getId())) {
            List<Lesson> tl = lessonsByTopic.getOrDefault(t.getId(), List.of());
            boolean chapterUnlocked = previousCleared && !tl.isEmpty();
            List<StageNode> stages = new ArrayList<>();
            int starsEarned = 0;
            for (Lesson l : tl) {
                level++;
                StageResult r = byLesson.get(l.getId());
                int stars = r == null ? 0 : r.getStars();
                boolean cleared = stars > 0;
                boolean open = previousCleared;
                String status = cleared ? "CLEARED" : open ? "OPEN" : "LOCKED";
                boolean current = open && !cleared && map.next == null;
                if (current) map.next = new Slot(LESSON, l.getId(), l.getTitle(), level, stars);
                map.slots.put(LESSON + ":" + l.getId(), new Slot(LESSON, l.getId(), l.getTitle(), level, stars, open || cleared));
                stages.add(new StageNode(l.getId(), level, l.getTitle(), stars, status, current,
                        questionCount.getOrDefault(l.getId(), 0L).intValue(), l.getDurationMinutes()));
                starsEarned += stars;
                previousCleared = cleared;
            }

            BossNode boss = null;
            long pool = topicQuestionCount.computeIfAbsent(t.getId(),
                    id -> (long) questionRepository.findByTopicIdAndReviewStatus(id, ReviewStatus.PUBLISHED).size());
            if (!tl.isEmpty() && pool >= 3) {
                level++;
                StageResult r = byTopic.get(t.getId());
                int stars = r == null ? 0 : r.getStars();
                boolean cleared = stars > 0;
                boolean open = previousCleared;
                boolean current = open && !cleared && map.next == null;
                String title = "Ải trùm " + shortChapter(t);
                if (current) map.next = new Slot(BOSS, t.getId(), title, level, stars);
                map.slots.put(BOSS + ":" + t.getId(), new Slot(BOSS, t.getId(), title, level, stars, open || cleared));
                Pet pet = t.getRewardPetCode() == null ? null : pets.get(t.getRewardPetCode());
                boss = new BossNode(t.getId(), level, cleared ? "CLEARED" : open ? "OPEN" : "LOCKED", current, stars,
                        pet != null ? pet.getCode() : null, pet != null ? pet.getName() : null, BOSS_CHEST_COINS);
                starsEarned += stars;
                previousCleared = cleared;
            }
            int starsTotal = (stages.size() + (boss != null ? 1 : 0)) * 3;
            map.chapters.add(new ChapterNode(t.getId(), t.getOrderIndex(), t.getName(), t.getDescription(),
                    t.getSemester() == null ? 1 : t.getSemester(), chapterUnlocked, starsEarned, starsTotal, stages, boss));
        }
        return map;
    }

    /** "Chương I. Phương trình..." -> "Chương I" */
    private static String shortChapter(Topic t) {
        String n = t.getName();
        int dot = n.indexOf('.');
        return dot > 0 && n.startsWith("Chương") ? n.substring(0, dot) : "chủ đề " + t.getOrderIndex();
    }

    // ---------------------------------------------------------------

    private static final class GameMap {
        LearningPath path;
        final List<ChapterNode> chapters = new ArrayList<>();
        final Map<String, Slot> slots = new HashMap<>();
        Slot next;

        Slot require(String type, String id) {
            Slot s = slots.get(type + ":" + id);
            if (s == null) throw ApiException.notFound("Không tìm thấy ải trong lộ trình của bạn");
            if (!s.playable) throw ApiException.forbidden("Ải này chưa mở khoá. Hãy qua ải trước đó nhé!");
            return s;
        }
    }

    private static final class Slot {
        final String type;
        final String id;
        final String title;
        final int levelNumber;
        final int stars;
        final boolean playable;

        Slot(String type, String id, String title, int levelNumber, int stars) {
            this(type, id, title, levelNumber, stars, true);
        }

        Slot(String type, String id, String title, int levelNumber, int stars, boolean playable) {
            this.type = type;
            this.id = id;
            this.title = title;
            this.levelNumber = levelNumber;
            this.stars = stars;
            this.playable = playable;
        }
    }

    private static final class Grade {
        int correct;
        int total;
        final List<AnswerFeedback> feedback = new ArrayList<>();
    }

    private static final class Reward {
        int previousStars;
        int coins;
        int xp;
        boolean firstClear;
        boolean levelUp;
    }
}

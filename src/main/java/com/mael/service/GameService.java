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
        return myPets();
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
        return new GameProfile(s.getFullName(), s.getGradeLevel(), Boolean.TRUE.equals(s.getOnboarded()),
                s.getLearningGoal() != null ? s.getLearningGoal().name() : null,
                s.getCoins() == null ? 0 : s.getCoins(), xp, xp / XP_PER_LEVEL + 1, xp % XP_PER_LEVEL, XP_PER_LEVEL,
                totalStars, s.getActivePetCode(), petName);
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
        studentRepository.save(student);
        // Tính lại bản đồ sau khi lưu kết quả để biết ải tiếp theo
        GameMap after = buildMap(student);
        Slot next = after.next;
        return new StageResultView(type, grade.correct, grade.total, stars, reward.previousStars, stars > 0,
                reward.firstClear, reward.coins, reward.xp, reward.levelUp, rewardPet, grade.feedback,
                next != null ? next.type : null, next != null ? next.id : null, next != null ? next.title : null,
                profile(student));
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

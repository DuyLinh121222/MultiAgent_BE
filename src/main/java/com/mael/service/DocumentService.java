package com.mael.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.mael.dto.document.DocumentView;
import com.mael.dto.document.TopicOption;
import com.mael.entity.*;
import com.mael.entity.enums.*;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import com.mael.service.ai.gemini.GeminiClient;
import com.mael.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Giảng viên tải tài liệu PDF lên -> AI (key GEMINI_KEY_DOCUMENT) đọc PDF và soạn:
 * phần lý thuyết tóm tắt, các lỗi sai hay gặp (cho bạn AI phản biện), câu trắc nghiệm ngắn (cho ải và mini game Đấu trường).
 *
 * Mọi thứ AI soạn đều ở trạng thái CHỜ DUYỆT, giảng viên duyệt xong học sinh mới thấy.
 * AI chạy nền (vài chục giây), FE hỏi lại trạng thái tài liệu (aiStatus) để cập nhật.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_PDF_BYTES = 12L * 1024 * 1024;
    private static final int MAX_QUESTIONS = 12;

    private final FileStorageService fileStorageService;
    private final LessonMaterialRepository materialRepository;
    private final LessonRepository lessonRepository;
    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final CurrentUser currentUser;
    private final GeminiClient gemini;
    private final TaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public List<TopicOption> topics() {
        return topicRepository.findAll().stream()
                .sorted(Comparator.comparing((Topic t) -> t.getLearningPath().getGradeLevel()).reversed()
                        .thenComparing(Topic::getOrderIndex))
                .map(t -> new TopicOption(t.getId(), t.getName(), t.getLearningPath().getGradeLevel(),
                        t.getLessons().stream()
                                .sorted(Comparator.comparing(Lesson::getOrderIndex))
                                .map(l -> new TopicOption.LessonOption(l.getId(), l.getTitle(), l.getReviewStatus().name()))
                                .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentView> myDocuments() {
        Lecturer lecturer = currentUser.lecturer();
        return materialRepository.findByLecturer(lecturer.getId()).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public DocumentView get(String id) {
        return toView(ownMaterial(id));
    }

    /**
     * Tải PDF lên. lessonId có giá trị = gắn vào bài có sẵn, để trống = tạo bài (ải) mới trong chương topicId.
     */
    @Transactional
    public DocumentView upload(MultipartFile file, String topicId, String lessonId, String title) {
        Lecturer lecturer = currentUser.lecturer();
        Lesson lesson;
        if (lessonId != null && !lessonId.isBlank()) {
            lesson = lessonRepository.findById(lessonId).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài học"));
        } else {
            Topic topic = topicRepository.findById(topicId == null ? "" : topicId)
                    .orElseThrow(() -> ApiException.badRequest("Vui lòng chọn chương để gắn tài liệu"));
            String name = title != null && !title.isBlank() ? title.trim()
                    : Optional.ofNullable(file.getOriginalFilename()).orElse("Bài mới").replaceAll("(?i)\\.pdf$", "");
            lesson = lessonRepository.save(Lesson.builder()
                    .topic(topic)
                    .title(name.length() > 250 ? name.substring(0, 250) : name)
                    .content("Đang chờ AI đọc tài liệu PDF...")
                    .durationMinutes(10)
                    .orderIndex(lessonRepository.findMaxOrderIndex(topic.getId()) + 1)
                    .createdBy(lecturer.getUser())
                    .reviewStatus(ReviewStatus.DRAFT)
                    .build());
        }
        FileStorageService.StoredFile stored = fileStorageService.storePdf(file, "materials", MAX_PDF_BYTES);
        LessonMaterial m = materialRepository.save(LessonMaterial.builder()
                .lesson(lesson)
                .fileName(Optional.ofNullable(file.getOriginalFilename()).orElse("tai-lieu.pdf"))
                .fileUrl(stored.url())
                .fileType(FileType.PDF)
                .fileSize(file.getSize())
                .uploadedBy(lecturer)
                .aiStatus("PROCESSING")
                .aiNote("AI đang đọc tài liệu, khoảng 20-60 giây...")
                .build());
        scheduleProcessing(m.getId(), lecturer.getUser().getId());
        return toView(m);
    }

    /** Cho AI đọc lại tài liệu (ví dụ lần trước lỗi do chưa có key) */
    @Transactional
    public DocumentView reprocess(String id) {
        LessonMaterial m = ownMaterial(id);
        if ("PROCESSING".equals(m.getAiStatus())) throw ApiException.conflict("AI đang đọc tài liệu này rồi");
        m.setAiStatus("PROCESSING");
        m.setAiNote("AI đang đọc lại tài liệu...");
        scheduleProcessing(m.getId(), m.getUploadedBy().getUser().getId());
        return toView(m);
    }

    /** Duyệt nhanh: xuất bản bài + mọi câu hỏi đang chờ duyệt của tài liệu */
    @Transactional
    public DocumentView approveAll(String id) {
        Lecturer lecturer = currentUser.lecturer();
        LessonMaterial m = ownMaterial(id);
        Lesson lesson = m.getLesson();
        LocalDateTime now = LocalDateTime.now();
        if (lesson.getReviewStatus() == ReviewStatus.PENDING_REVIEW) {
            lesson.setReviewStatus(ReviewStatus.PUBLISHED);
            lesson.setReviewedBy(lecturer);
            lesson.setReviewedAt(now);
        }
        for (Question q : questionRepository.findByLessonIdAndReviewStatusOrderByCreatedAtAsc(lesson.getId(), ReviewStatus.PENDING_REVIEW)) {
            q.setReviewStatus(ReviewStatus.PUBLISHED);
            q.setReviewedBy(lecturer);
            q.setReviewedAt(now);
        }
        return toView(m);
    }

    // ------------------------------------------------------------------

    private void scheduleProcessing(String materialId, String userId) {
        Runnable job = () -> taskExecutor.execute(() -> process(materialId, userId));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
        } else {
            job.run();
        }
    }

    private void process(String materialId, String userId) {
        Result result;
        try {
            result = callAi(materialId);
        } catch (Exception e) {
            log.warn("AI đọc PDF lỗi: {}", e.getMessage());
            result = Result.fail("AI chưa đọc được tài liệu: " + e.getMessage());
        }
        Result r = result;
        transactionTemplate.executeWithoutResult(tx -> save(materialId, userId, r));
    }

    private record Result(boolean ok, String note, JsonNode json) {
        static Result fail(String note) {
            return new Result(false, note, null);
        }
    }

    private Result callAi(String materialId) throws Exception {
        if (!gemini.enabled(GeminiClient.Task.DOCUMENT)) {
            return Result.fail("Chưa bật AI (cần AI_PROVIDER=gemini và GEMINI_KEY_DOCUMENT trong file .env). "
                    + "Tài liệu đã được lưu, bấm \"AI đọc lại\" sau khi cấu hình.");
        }
        String[] info = transactionTemplate.execute(tx -> {
            LessonMaterial m = materialRepository.findById(materialId).orElseThrow();
            Lesson l = m.getLesson();
            return new String[]{m.getFileUrl(), l.getTitle(), l.getTopic().getName(),
                    String.valueOf(l.getTopic().getLearningPath().getGradeLevel())};
        });
        byte[] pdf = Files.readAllBytes(fileStorageService.resolveUrl(info[0]));
        String system = """
                Bạn là giáo viên Toán THCS Việt Nam (chương trình GDPT 2018) soạn học liệu cho web học kiểu trò chơi.
                CHỈ dùng kiến thức có trong tài liệu PDF đính kèm. Không bịa nội dung ngoài tài liệu.
                Trả về JSON đúng mẫu:
                {
                  "title": "tên bài ngắn gọn",
                  "content": "lý thuyết tóm tắt, mỗi ý 1 dòng bắt đầu bằng '• ', có 1 dòng 'Ví dụ: ...', tối đa 12 dòng",
                  "misconceptions": [{"wrong":"một câu phát biểu SAI học sinh hay nhầm","why":"vì sao hay nhầm","fix":"phát biểu đúng"}],
                  "questions": [{"content":"đề ngắn","options":["...","...","...","..."],"correctIndex":0,"explanation":"lời giải 1-2 câu","difficulty":"EASY|MEDIUM|HARD"}]
                }
                Yêu cầu câu hỏi (dùng cho mini game, học sinh lười đọc chữ):
                - 6 đến 12 câu, đề NGẮN (ưu tiên dưới 80 kí tự, viết dạng biểu thức/phép tính khi có thể).
                - Đúng 4 lựa chọn, CÁC LỰA CHỌN DÀI GẦN BẰNG NHAU; đáp án đúng KHÔNG được dài hơn hẳn các đáp án khác.
                - Lựa chọn sai phải là lỗi sai thường gặp, nghe có lý. Không dùng "Cả A và B", "Tất cả đều đúng".
                - Không ghi chữ A/B/C/D ở đầu lựa chọn. Dùng kí hiệu √ ² ³ ≤ ≥ ≠ π, không dùng LaTeX.
                - misconceptions: 3 đến 5 ý, "wrong" và "fix" dài tương đương nhau.
                """;
        String prompt = "Tài liệu cho học sinh lớp " + info[3] + ", chương \"" + info[2] + "\", bài \"" + info[1] + "\".";
        Optional<JsonNode> json = gemini.generateJson(GeminiClient.Task.DOCUMENT, system,
                List.of(GeminiClient.Part.file("application/pdf", pdf), GeminiClient.Part.text(prompt)), 0.3, 6000);
        if (json.isEmpty()) return Result.fail("AI không trả lời (có thể hết lượt miễn phí hoặc file quá dài). Thử lại sau ít phút.");
        return new Result(true, null, json.get());
    }

    private void save(String materialId, String userId, Result r) {
        LessonMaterial m = materialRepository.findById(materialId).orElse(null);
        if (m == null) return;
        if (!r.ok()) {
            m.setAiStatus("FAILED");
            m.setAiNote(r.note());
            return;
        }
        JsonNode j = r.json();
        Lesson lesson = m.getLesson();
        boolean newLesson = lesson.getReviewStatus() == ReviewStatus.DRAFT;
        String content = j.path("content").asText("").trim();
        if (!content.isEmpty() && (newLesson || lesson.getContent() == null || lesson.getContent().isBlank())) {
            lesson.setContent(content);
        }
        if (newLesson && !j.path("title").asText("").isBlank() && lesson.getTitle().equalsIgnoreCase(stripPdf(m.getFileName()))) {
            lesson.setTitle(abbreviate(j.path("title").asText(), 250));
        }
        List<Misconception> mis = new ArrayList<>();
        for (JsonNode x : j.path("misconceptions")) {
            String w = x.path("wrong").asText("").trim(), why = x.path("why").asText("").trim(), fix = x.path("fix").asText("").trim();
            if (!w.isEmpty() && !fix.isEmpty()) mis.add(new Misconception(w, why.isEmpty() ? "Dễ nhầm khi làm nhanh." : why, fix));
        }
        if (!mis.isEmpty() && (newLesson || lesson.getMisconceptions() == null || lesson.getMisconceptions().isEmpty())) {
            lesson.setMisconceptions(mis);
        }
        if (newLesson) lesson.setReviewStatus(ReviewStatus.PENDING_REVIEW);

        UserAccount author = m.getUploadedBy() != null ? m.getUploadedBy().getUser() : null;
        int saved = 0;
        int skipped = 0;
        Random rnd = new Random();
        for (JsonNode q : j.path("questions")) {
            if (saved >= MAX_QUESTIONS) break;
            String text = q.path("content").asText("").trim();
            List<String> opts = new ArrayList<>();
            q.path("options").forEach(o -> opts.add(o.asText("").replaceFirst("^\\s*[A-Da-d][.)]\\s*", "").trim()));
            int correct = q.path("correctIndex").asInt(-1);
            if (text.isEmpty() || opts.size() != 4 || correct < 0 || correct > 3
                    || opts.stream().anyMatch(String::isBlank) || new HashSet<>(opts).size() != 4
                    || longestGivesAway(opts, correct)) {
                skipped++;
                continue;
            }
            // Xáo vị trí đáp án đúng
            String right = opts.get(correct);
            Collections.shuffle(opts, rnd);
            int idx = opts.indexOf(right);
            List<String> labelled = new ArrayList<>();
            for (int i = 0; i < 4; i++) labelled.add((char) ('A' + i) + ". " + opts.get(i));
            Difficulty d;
            try {
                d = Difficulty.valueOf(q.path("difficulty").asText("MEDIUM").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                d = Difficulty.MEDIUM;
            }
            questionRepository.save(Question.builder()
                    .topic(lesson.getTopic())
                    .lesson(lesson)
                    .content(text)
                    .questionType(QuestionType.SINGLE_CHOICE)
                    .options(labelled)
                    .correctAnswer(String.valueOf((char) ('A' + idx)))
                    .explanation(q.path("explanation").asText(""))
                    .difficulty(d)
                    .source(ContentSource.AI)
                    .createdBy(author)
                    .reviewStatus(ReviewStatus.PENDING_REVIEW)
                    .build());
            saved++;
        }
        m.setAiStatus(saved > 0 || !content.isEmpty() ? "DONE" : "FAILED");
        m.setAiNote("AI đã soạn " + saved + " câu hỏi" + (mis.isEmpty() ? "" : ", " + mis.size() + " lỗi sai hay gặp")
                + (skipped > 0 ? " (bỏ " + skipped + " câu chưa đạt chuẩn, ví dụ đáp án đúng dài lộ liễu)" : "")
                + ". Vào tab \"Câu hỏi chờ duyệt\" để duyệt, hoặc bấm \"Duyệt tất cả\".");
    }

    /** Đáp án đúng dài hơn hẳn mọi đáp án khác -> học sinh đoán được, bỏ câu này */
    static boolean longestGivesAway(List<String> opts, int correct) {
        int right = opts.get(correct).length();
        int maxOther = 0;
        for (int i = 0; i < opts.size(); i++) if (i != correct) maxOther = Math.max(maxOther, opts.get(i).length());
        return right > 12 && right > maxOther * 1.4;
    }

    private LessonMaterial ownMaterial(String id) {
        Lecturer lecturer = currentUser.lecturer();
        return materialRepository.findById(id)
                .filter(m -> m.getUploadedBy() != null && m.getUploadedBy().getId().equals(lecturer.getId()))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài liệu"));
    }

    private DocumentView toView(LessonMaterial m) {
        Lesson l = m.getLesson();
        return new DocumentView(m.getId(), m.getFileName(), m.getFileUrl(), m.getFileSize() == null ? 0 : m.getFileSize(),
                l.getId(), l.getTitle(), l.getReviewStatus().name(), l.getTopic().getId(), l.getTopic().getName(),
                m.getAiStatus(), m.getAiNote(),
                questionRepository.countByLessonIdAndReviewStatus(l.getId(), ReviewStatus.PENDING_REVIEW),
                questionRepository.countByLessonIdAndReviewStatus(l.getId(), ReviewStatus.PUBLISHED),
                m.getUploadedAt());
    }

    private static String stripPdf(String name) {
        return name == null ? "" : name.replaceAll("(?i)\\.pdf$", "");
    }

    private static String abbreviate(String s, int max) {
        return s.length() > max ? s.substring(0, max) : s;
    }
}

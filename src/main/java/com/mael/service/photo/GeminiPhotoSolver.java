package com.mael.service.photo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mael.service.ai.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Giải toán từ ảnh bằng Gemini (key GEMINI_KEY_PHOTO). Chưa bật Gemini hoặc gọi lỗi thì dùng lời giải mẫu.
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class GeminiPhotoSolver implements PhotoSolver {

    private static final String SYSTEM = """
            Bạn là giáo viên Toán THCS Việt Nam. Đọc đề toán trong ảnh và giải theo chương trình GDPT 2018.
            Trả về JSON đúng mẫu:
            {"extractedText":"đề bài chép lại","topicHint":"tên chủ đề ngắn, ví dụ Căn bậc hai",
             "steps":["Bước 1: ...","Bước 2: ..."],"finalAnswer":"đáp số ngắn gọn"}
            Quy tắc: mỗi bước 1-2 câu, dùng kí hiệu √ ² ³ ≤ ≥ ≠ thay vì LaTeX, tối đa 8 bước.
            Bước cuối là bước "Thử lại" kiểm tra đáp số. Nếu ảnh không phải bài toán thì extractedText = "" và steps = [].
            """;

    private final GeminiClient gemini;
    private final MockPhotoSolver fallback;

    @Override
    public SolveResult solve(Path imagePath, String studentNote, int gradeLevel) {
        if (!gemini.enabled(GeminiClient.Task.PHOTO)) {
            return fallback.solve(imagePath, studentNote, gradeLevel);
        }
        try {
            byte[] bytes = Files.readAllBytes(imagePath);
            String name = imagePath.getFileName().toString().toLowerCase(Locale.ROOT);
            String mime = name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp"
                    : name.endsWith(".heic") ? "image/heic" : "image/jpeg";
            String prompt = "Học sinh lớp " + gradeLevel + " gửi ảnh bài toán."
                    + (studentNote != null && !studentNote.isBlank() ? " Ghi chú của học sinh: " + studentNote : "");
            Optional<JsonNode> json = gemini.generateJson(GeminiClient.Task.PHOTO, SYSTEM,
                    List.of(GeminiClient.Part.file(mime, bytes), GeminiClient.Part.text(prompt)), 0.2, 1500);
            if (json.isPresent()) {
                JsonNode j = json.get();
                List<String> steps = new ArrayList<>();
                j.path("steps").forEach(s -> steps.add(s.asText()));
                if (!steps.isEmpty()) {
                    return new SolveResult(j.path("extractedText").asText(studentNote),
                            j.path("topicHint").asText(null), steps, j.path("finalAnswer").asText(""), gemini.modelName());
                }
                throw new IllegalStateException("Không đọc được bài toán trong ảnh");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini giải ảnh lỗi: {}", e.getMessage());
        }
        return fallback.solve(imagePath, studentNote, gradeLevel);
    }
}

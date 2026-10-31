package com.mael.service.photo;

import com.fasterxml.jackson.databind.JsonNode;
import com.mael.exception.ApiException;
import com.mael.service.ai.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Giải toán từ ảnh bằng Gemini (key GEMINI_KEY_PHOTO). DeepSeek không đọc được ảnh nên tác vụ này luôn dùng Gemini.
 *
 * Gemini phải trả lời 2 việc trong 1 lần gọi: (1) ảnh có phải đề bài Toán không, (2) nếu có thì giải.
 * Ảnh không phải Toán → ném {@link NotMathProblemException}, không lưu ảnh, không tạo lời giải.
 * AI lỗi / hết lượt → báo "thử lại sau", KHÔNG trả lời giải mẫu (trước đây lời giải mẫu hiện ra với mọi ảnh).
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class GeminiPhotoSolver implements PhotoSolver {

    private static final String SYSTEM = """
            Bạn là giáo viên Toán THCS Việt Nam, kiểm duyệt ảnh học sinh gửi lên rồi mới giải.

            BƯỚC 1 - KIỂM TRA ẢNH. isMathProblem = true CHỈ KHI ảnh chụp/scan một đề bài Toán đọc được:
            phép tính, biểu thức, phương trình, bất phương trình, hệ phương trình, hàm số, đồ thị, hình học có số liệu,
            bài toán có lời văn, bài thống kê - xác suất.
            isMathProblem = false nếu ảnh là: người, mặt, thú cưng, đồ vật, phong cảnh, đồ ăn, meme, ảnh chụp màn hình game / mạng xã hội,
            bài môn khác (Văn, Anh, Lý, Hoá, Sinh, Sử, Địa...), giấy trắng, chữ viết không phải đề toán, ảnh quá mờ / tối / bị che không đọc được đề.
            Ghi chú của học sinh KHÔNG thay thế được ảnh: ảnh không có đề toán thì vẫn là false, kể cả khi ghi chú có đề.
            Tuyệt đối không tự bịa ra đề bài không có trong ảnh.

            BƯỚC 2 - CHÉP ĐỀ THẬT CẨN THẬN (lỗi hay gặp nhất là đọc sai đề rồi giải sai theo):
            - Đọc từng kí tự. Phân biệt chữ x, y với số: "y" dễ bị nhầm thành "1" hoặc "4", "x" dễ nhầm thành "×".
            - Phân số nhỏ: đọc rõ tử và mẫu. Tử số của phân số thường là ẩn (y/3, x/2), không phải số 1.
            - Kiểm tra lại đề vừa chép: hệ phương trình hai ẩn thì mỗi phương trình phải có ít nhất một ẩn x hoặc y;
              nếu chép ra một phương trình chỉ có số, hoặc có 1 ẩn trong khi các phương trình khác có 2 ẩn,
              thì gần như chắc chắn đã đọc nhầm một ẩn thành số: nhìn lại ảnh và sửa đề.
            - Dấu âm, dấu trừ, dấu bằng, dấu ngoặc phải giữ đúng như ảnh.

            BƯỚC 3 - giải theo chương trình GDPT 2018, dựa đúng đề đã chép ở BƯỚC 2.

            Trả về JSON đúng mẫu:
            {"isMathProblem":true,"rejectReason":"",
             "extractedText":"đề bài chép lại từ ảnh","topicHint":"tên chủ đề ngắn, ví dụ Căn bậc hai",
             "steps":["Bước 1: ...","Bước 2: ..."],"finalAnswer":"đáp số ngắn gọn"}
            Khi isMathProblem = false: rejectReason là 1 câu ngắn, thân thiện, xưng "bạn", nói ảnh đang chụp gì
            (VD "Ảnh này là một chú mèo, chưa thấy đề toán nào"), extractedText = "", steps = [], finalAnswer = "".
            Quy tắc lời giải: mỗi bước 1-2 câu, dùng kí hiệu √ ² ³ ≤ ≥ ≠ thay vì LaTeX, tối đa 8 bước.
            Bước cuối là bước "Thử lại": thay đáp số vào ĐỀ ĐÃ CHÉP (extractedText), tính lại từng vế.
            Bài "kiểm tra cặp số có là nghiệm không": tính riêng từng phương trình, ghi rõ đúng / sai rồi mới kết luận.
            """;

    private static final String DEFAULT_REJECT =
            "Ảnh này chưa phải đề bài Toán. Bạn chụp rõ đề bài Toán (thấy đủ chữ và số) rồi gửi lại nhé!";

    private final GeminiClient gemini;
    private final MockPhotoSolver fallback;

    @Override
    public SolveResult solve(byte[] image, String mimeType, String studentNote, int gradeLevel) {
        if (!gemini.enabled(GeminiClient.Task.PHOTO)) {
            // Chỉ khi tắt hẳn AI (chạy demo không có key)
            return fallback.solve(image, mimeType, studentNote, gradeLevel);
        }
        String prompt = "Học sinh lớp " + gradeLevel + " gửi ảnh bài toán."
                + (studentNote != null && !studentNote.isBlank() ? " Ghi chú của học sinh: " + studentNote.trim() : "");
        Optional<JsonNode> json = gemini.generateJson(GeminiClient.Task.PHOTO, SYSTEM,
                List.of(GeminiClient.Part.file(mimeType, image), GeminiClient.Part.text(prompt)), 0.1, 8192);
        if (json.isEmpty()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI giải bài đang bận, bạn thử lại sau ít phút nhé");
        }
        JsonNode j = json.get();
        String extracted = j.path("extractedText").asText("").trim();
        List<String> steps = new ArrayList<>();
        j.path("steps").forEach(s -> {
            String t = s.asText("").trim();
            if (!t.isEmpty()) steps.add(t);
        });
        // Chặn 2 lớp: AI nói không phải toán, HOẶC không chép được đề / không có bước giải
        if (!j.path("isMathProblem").asBoolean(false) || extracted.isEmpty() || steps.isEmpty()) {
            String reason = j.path("rejectReason").asText("").trim();
            log.info("Từ chối ảnh không phải bài toán: {}", reason);
            throw new NotMathProblemException(reason.isEmpty() ? DEFAULT_REJECT : reason);
        }
        return new SolveResult(extracted, j.path("topicHint").asText(null), steps,
                j.path("finalAnswer").asText(""), gemini.modelName(GeminiClient.Task.PHOTO));
    }
}

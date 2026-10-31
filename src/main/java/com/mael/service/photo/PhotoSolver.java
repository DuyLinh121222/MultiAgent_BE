package com.mael.service.photo;

import java.util.List;

/**
 * Giải bài toán từ ảnh. Bản dùng thật là {@link GeminiPhotoSolver}; {@link MockPhotoSolver} chỉ dùng khi tắt AI (AI_PROVIDER=mock).
 */
public interface PhotoSolver {

    /**
     * @param image     nội dung ảnh (chưa lưu ở đâu cả)
     * @param mimeType  image/jpeg, image/png...
     * @throws NotMathProblemException ảnh không phải bài tập Toán: không lưu ảnh, không tạo lời giải
     */
    SolveResult solve(byte[] image, String mimeType, String studentNote, int gradeLevel);

    /**
     * @param topicHint tên chủ đề gợi ý để map sang TOPIC (có thể null)
     */
    record SolveResult(String extractedText, String topicHint, List<String> steps, String finalAnswer, String modelName) {
    }

    /** Ảnh không phải đề Toán (ảnh người, đồ vật, meme, môn khác, ảnh mờ...) */
    class NotMathProblemException extends RuntimeException {
        public NotMathProblemException(String reason) {
            super(reason);
        }
    }
}

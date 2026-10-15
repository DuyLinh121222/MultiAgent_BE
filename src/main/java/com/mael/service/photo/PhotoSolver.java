package com.mael.service.photo;

import java.nio.file.Path;
import java.util.List;

/**
 * Giải bài toán từ ảnh. Bản hiện tại là {@link MockPhotoSolver}.
 * Khi tích hợp OpenAI (gpt-6.1-sol, reasoning high): gửi ảnh + ghi chú, yêu cầu trả JSON
 * {extractedText, topicHint, steps[], finalAnswer}.
 */
public interface PhotoSolver {

    SolveResult solve(Path imagePath, String studentNote, int gradeLevel);

    /**
     * @param topicHint tên chủ đề gợi ý để map sang TOPIC (có thể null)
     */
    record SolveResult(String extractedText, String topicHint, List<String> steps, String finalAnswer, String modelName) {
    }
}

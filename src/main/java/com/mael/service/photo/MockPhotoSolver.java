package com.mael.service.photo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * Lời giải mẫu để chạy thử luồng upload ảnh khi chưa tích hợp AI.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockPhotoSolver implements PhotoSolver {

    @Override
    public SolveResult solve(Path imagePath, String studentNote, int gradeLevel) {
        String text = (studentNote != null && !studentNote.isBlank())
                ? studentNote.trim()
                : "Tìm x biết |2x - 3| = 7";
        return new SolveResult(
                text,
                "Giá trị tuyệt đối",
                List.of(
                        "Bước 1: Thầy Minh Trí - Xác định dạng bài: |A| = m với m = 7 ≥ 0 nên có 2 trường hợp.",
                        "Bước 2: Trường hợp 1: 2x - 3 = 7 ⇒ 2x = 10 ⇒ x = 5.",
                        "Bước 3: Trường hợp 2: 2x - 3 = -7 ⇒ 2x = -4 ⇒ x = -2.",
                        "Bước 4: Bạn Soát Lỗi - Thử lại: |2·5 - 3| = 7 ✓ và |2·(-2) - 3| = |-7| = 7 ✓.",
                        "Lưu ý: Đây là lời giải mẫu (chưa tích hợp AI đọc ảnh)."
                ),
                "x = 5 hoặc x = -2",
                "mock-solver");
    }
}

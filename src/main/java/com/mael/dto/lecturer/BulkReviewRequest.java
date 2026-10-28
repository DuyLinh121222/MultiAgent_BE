package com.mael.dto.lecturer;

import com.mael.entity.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Duyệt / từ chối nhiều nội dung cùng lúc (ví dụ duyệt 10 câu AI soạn từ 1 file PDF) */
public record BulkReviewRequest(
        @Size(max = 200) List<String> lessonIds,
        @Size(max = 200) List<String> questionIds,
        @NotNull ReviewDecision decision,
        @Size(max = 1000) String comment
) {
}

package com.mael.dto.lecturer;

import com.mael.entity.Misconception;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Soạn / sửa bài (ải): lý thuyết tóm tắt + các lỗi sai hay gặp (AI phản biện dựa vào đây) */
public record LessonUpsertRequest(
        @NotBlank(message = "Chọn chương") String topicId,
        @NotBlank(message = "Nhập tên bài") @Size(max = 255) String title,
        @Size(max = 20000) String content,
        @Size(max = 500) String videoUrl,
        Integer durationMinutes,
        List<Misconception> misconceptions,
        Boolean publish
) {
}

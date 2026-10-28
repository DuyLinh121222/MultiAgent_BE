package com.mael.dto.lecturer;

import com.mael.entity.enums.Difficulty;
import com.mael.entity.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Giảng viên soạn / sửa câu hỏi. lessonId bỏ trống = câu của ải trùm (theo chương).
 * publish = true: xuất bản luôn (giảng viên là người duyệt); false: lưu nháp.
 */
public record QuestionUpsertRequest(
        @NotBlank(message = "Chọn chương") String topicId,
        String lessonId,
        @NotBlank(message = "Nhập nội dung câu hỏi") @Size(max = 2000) String content,
        @NotNull QuestionType questionType,
        List<@Size(max = 500) String> options,
        @NotBlank(message = "Nhập đáp án đúng") @Size(max = 255) String correctAnswer,
        @Size(max = 2000) String explanation,
        Difficulty difficulty,
        Boolean publish
) {
}

package com.mael.dto.lecturer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thêm / sửa chương. pathId chỉ cần khi tạo mới */
public record TopicUpsertRequest(
        String pathId,
        @NotBlank(message = "Nhập tên chương") @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @Min(1) @Max(2) Integer semester
) {
}

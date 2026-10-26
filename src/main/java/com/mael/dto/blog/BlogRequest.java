package com.mael.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BlogRequest(
        @NotBlank(message = "Vui lòng nhập tiêu đề") @Size(max = 200, message = "Tiêu đề tối đa 200 kí tự") String title,
        @Size(max = 500, message = "Tóm tắt tối đa 500 kí tự") String summary,
        @NotBlank(message = "Vui lòng nhập nội dung") @Size(max = 20000, message = "Nội dung quá dài") String content,
        @Size(max = 1000) @Pattern(regexp = "^$|^https?://.+", message = "Ảnh bìa phải là đường link http(s)") String coverUrl,
        @Pattern(regexp = "^(BLUE|ORANGE|GREEN|PURPLE|PINK)?$", message = "Màu banner không hợp lệ") String theme,
        @Size(max = 50) String tag,
        Boolean published,
        Boolean pinned
) {
}

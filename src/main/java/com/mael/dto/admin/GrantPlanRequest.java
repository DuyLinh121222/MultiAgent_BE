package com.mael.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Admin kích hoạt gói thủ công cho học sinh (học sinh của trung tâm / trường, khuyến mãi, bù lỗi thanh toán) */
public record GrantPlanRequest(
        @NotBlank(message = "Chọn gói") String planId,
        @Size(max = 300) String note
) {
}

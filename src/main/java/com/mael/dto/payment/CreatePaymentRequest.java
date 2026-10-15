package com.mael.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentRequest(
        @NotBlank(message = "Vui lòng chọn gói") String planId
) {
}

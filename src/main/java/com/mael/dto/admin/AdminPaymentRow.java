package com.mael.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentRow(
        String id,
        String userId,
        String studentName,
        String email,
        String planName,
        BigDecimal amount,
        String method,
        String status,
        String transactionCode,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}

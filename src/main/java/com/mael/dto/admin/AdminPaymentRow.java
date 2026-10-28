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
        LocalDateTime paidAt,
        /** Nội dung chuyển khoản để đối chiếu trong app MoMo */
        String transferCode,
        /** Học sinh bấm "Tôi đã chuyển khoản" lúc nào */
        LocalDateTime customerPaidAt,
        String adminNote
) {
}

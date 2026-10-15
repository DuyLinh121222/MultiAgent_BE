package com.mael.dto.payment;

import com.mael.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * payUrl / qrCodeUrl: khi tích hợp MoMo sẽ là link thật MoMo trả về. Hiện tại để null.
 */
public record PaymentView(
        String id,
        String subscriptionId,
        String planName,
        BigDecimal amount,
        String method,
        PaymentStatus status,
        String payUrl,
        String qrCodeUrl,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}

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
        Integer durationDays,
        BigDecimal amount,
        String method,
        PaymentStatus status,
        String payUrl,
        String qrCodeUrl,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        /** Nội dung chuyển khoản học sinh phải ghi */
        String transferCode,
        /** Học sinh đã bấm "Tôi đã chuyển khoản" lúc nào (null = chưa) */
        LocalDateTime customerPaidAt,
        /** Lý do admin từ chối (khi FAILED) */
        String adminNote
) {
}

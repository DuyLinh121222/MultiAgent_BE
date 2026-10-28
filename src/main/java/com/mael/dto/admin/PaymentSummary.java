package com.mael.dto.admin;

import java.math.BigDecimal;

/** Tổng hợp giao dịch theo bộ lọc hiện tại */
public record PaymentSummary(
        long successCount,
        BigDecimal successAmount,
        long pendingCount,
        long failedCount,
        /** Học sinh báo đã chuyển, chờ admin xác nhận */
        long waitingCount,
        PageResponse<AdminPaymentRow> page
) {
}

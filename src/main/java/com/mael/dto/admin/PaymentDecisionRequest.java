package com.mael.dto.admin;

import jakarta.validation.constraints.Size;

/** Ghi chú khi xác nhận, hoặc lý do khi từ chối thanh toán */
public record PaymentDecisionRequest(@Size(max = 500) String note) {
}

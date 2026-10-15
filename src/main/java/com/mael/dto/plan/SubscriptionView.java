package com.mael.dto.plan;

import com.mael.entity.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SubscriptionView(
        String id,
        String planId,
        String planName,
        BigDecimal price,
        LocalDateTime startDate,
        LocalDateTime endDate,
        SubscriptionStatus status,
        long daysLeft,
        boolean trial
) {
}

package com.mael.dto.plan;

import java.math.BigDecimal;

public record PlanView(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        Integer maxAiSessions,
        boolean trial
) {
}

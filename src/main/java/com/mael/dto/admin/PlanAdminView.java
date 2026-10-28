package com.mael.dto.admin;

import com.mael.entity.enums.PlanAudience;
import com.mael.entity.enums.PriceUnit;

import java.math.BigDecimal;

/** Gói học nhìn từ phía admin: có cả gói đang ẩn và số liệu bán */
public record PlanAdminView(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        Integer maxAiSessions,
        PlanAudience audience,
        PriceUnit priceUnit,
        Integer minSeats,
        String features,
        boolean featured,
        int sortOrder,
        boolean active,
        long activeSubscribers,
        long soldCount,
        BigDecimal revenue
) {
}

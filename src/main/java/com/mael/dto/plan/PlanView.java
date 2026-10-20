package com.mael.dto.plan;

import com.mael.entity.enums.PlanAudience;
import com.mael.entity.enums.PriceUnit;

import java.math.BigDecimal;
import java.util.List;

/**
 * audience = STUDENT: mua qua MoMo. CENTER / SCHOOL: giá theo học sinh / tháng, gửi yêu cầu tư vấn.
 */
public record PlanView(
        String id,
        String name,
        String description,
        BigDecimal price,
        Integer durationDays,
        Integer maxAiSessions,
        boolean trial,
        PlanAudience audience,
        PriceUnit priceUnit,
        Integer minSeats,
        List<String> features,
        boolean featured
) {
}

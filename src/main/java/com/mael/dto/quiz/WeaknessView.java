package com.mael.dto.quiz;

import com.mael.entity.enums.Severity;
import com.mael.entity.enums.WeaknessStatus;

import java.math.BigDecimal;

public record WeaknessView(
        String id,
        String topicId,
        String topicName,
        BigDecimal masteryScore,
        Severity severity,
        WeaknessStatus status
) {
}

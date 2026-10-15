package com.mael.dto.common;

import java.time.LocalDateTime;

public record AchievementView(
        String id,
        String code,
        String name,
        String description,
        String iconUrl,
        Integer points,
        boolean earned,
        LocalDateTime earnedAt
) {
}

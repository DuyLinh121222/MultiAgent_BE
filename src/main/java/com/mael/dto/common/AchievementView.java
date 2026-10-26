package com.mael.dto.common;

import java.time.LocalDateTime;

/**
 * Huy hiệu và tiến độ của học sinh.
 *
 * @param iconUrl  tên icon Material Symbols
 * @param progress giá trị hiện tại của chỉ số (đã chặn trên bằng target)
 * @param target   mốc cần đạt (null = huy hiệu đặc biệt, không có thanh tiến độ)
 * @param tier     1 đồng, 2 bạc, 3 vàng, 4 kim cương
 */
public record AchievementView(
        String id,
        String code,
        String name,
        String description,
        String iconUrl,
        Integer points,
        boolean earned,
        LocalDateTime earnedAt,
        String criteria,
        String category,
        int tier,
        int progress,
        Integer target
) {
}

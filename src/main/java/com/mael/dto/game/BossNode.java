package com.mael.dto.game;

/**
 * Ải trùm cuối chương: thắng để mở rương báu (thú + xu).
 */
public record BossNode(
        String topicId,
        int levelNumber,
        String status,
        boolean current,
        int stars,
        String rewardPetCode,
        String rewardPetName,
        int rewardCoins
) {
}

package com.mael.dto.game;

import java.util.List;

/**
 * Một chương = một vùng đất trên bản đồ ải.
 */
public record ChapterNode(
        String topicId,
        int orderIndex,
        String name,
        String description,
        int semester,
        boolean unlocked,
        int starsEarned,
        int starsTotal,
        List<StageNode> stages,
        BossNode boss
) {
}

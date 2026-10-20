package com.mael.dto.game;

import com.mael.entity.Misconception;

import java.util.List;

/**
 * Nội dung một ải để chơi: lý thuyết (ải thường) + câu thử thách.
 *
 * @param stageType LESSON | BOSS
 * @param traps     các bẫy hay gặp của bài (hiện ở bước học lý thuyết)
 */
public record StageDetail(
        String stageType,
        String id,
        int levelNumber,
        String title,
        String chapterName,
        String content,
        Integer durationMinutes,
        List<Misconception> traps,
        int bestStars,
        List<StageQuestion> questions,
        String rewardPetCode,
        String rewardPetName
) {
}

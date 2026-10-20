package com.mael.dto.game;

import java.util.List;

/**
 * Kết quả sau khi nộp thử thách của một ải.
 *
 * @param rewardPet  thú nhận được từ rương báu (chỉ khi phá đảo ải trùm lần đầu)
 * @param nextStageType LESSON | BOSS | null
 */
public record StageResultView(
        String stageType,
        int correct,
        int total,
        int stars,
        int previousBestStars,
        boolean passed,
        boolean firstClear,
        int coinsEarned,
        int xpEarned,
        boolean levelUp,
        PetView rewardPet,
        List<AnswerFeedback> results,
        String nextStageType,
        String nextStageId,
        String nextStageTitle,
        GameProfile profile
) {
}

package com.mael.dto.game;

import com.mael.dto.common.AchievementView;

import java.util.List;

/**
 * Kết quả một lượt Đấu trường.
 *
 * @param coinCapReached đã nhận đủ xu tối đa trong ngày từ Đấu trường (vẫn được XP)
 */
public record ArenaResultView(
        int correct,
        int total,
        int bestCombo,
        int score,
        int bestScore,
        boolean newRecord,
        int coinsEarned,
        int xpEarned,
        boolean coinCapReached,
        boolean levelUp,
        List<AnswerFeedback> results,
        List<AchievementView> newAchievements,
        int streakDays,
        boolean streakIncreased,
        GameProfile profile
) {
}

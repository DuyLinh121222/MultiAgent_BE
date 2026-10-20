package com.mael.dto.game;

/**
 * Thông tin game của học sinh: xu, kinh nghiệm, cấp độ, thú đồng hành.
 * Cấp độ = xp / 100 + 1.
 */
public record GameProfile(
        String fullName,
        Integer gradeLevel,
        boolean onboarded,
        String learningGoal,
        int coins,
        int xp,
        int level,
        int xpInLevel,
        int xpPerLevel,
        int totalStars,
        String activePetCode,
        String activePetName
) {
}

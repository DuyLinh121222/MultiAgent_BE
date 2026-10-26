package com.mael.dto.game;

/**
 * Thông tin game của học sinh: xu, kinh nghiệm, cấp độ, thú đồng hành, thể lực, chuỗi ngày.
 * Cấp độ = xp / 100 + 1.
 *
 * @param nextEnergyInSeconds số giây nữa hồi thêm 1 thể lực (0 = đang đầy)
 * @param streakAtRisk        hôm qua có học nhưng hôm nay chưa: học ngay để giữ chuỗi
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
        String activePetName,
        int energy,
        int maxEnergy,
        long nextEnergyInSeconds,
        int streakDays,
        int bestStreak,
        boolean playedToday,
        boolean streakAtRisk,
        int streakShields
) {
}

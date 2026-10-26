package com.mael.service.game;

import com.mael.entity.Student;
import com.mael.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Thể lực và chuỗi ngày học.
 *
 * Thể lực (chống cày liên tục):
 * - Tối đa {@link #MAX_ENERGY} điểm. Mỗi lần nộp thử thách ải / ải trùm / Đấu trường tốn 1 điểm.
 * - Tự hồi 1 điểm mỗi {@link #REGEN_MINUTES} phút. Học lý thuyết và thảo luận với 3 AI không tốn thể lực.
 *
 * Chuỗi ngày (giữ chuỗi):
 * - Ngày nào có đi ải hoặc chơi Đấu trường thì chuỗi +1 (mỗi ngày chỉ tính 1 lần).
 * - Lỡ đúng 1 ngày mà còn khiên giữ chuỗi: tự dùng 1 khiên, chuỗi vẫn giữ. Lỡ hơn thế: chuỗi về 1.
 * - Khiên mua bằng xu, giữ tối đa {@link #MAX_SHIELDS} cái.
 */
@Service
public class StaminaService {

    public static final int MAX_ENERGY = 5;
    public static final int REGEN_MINUTES = 30;
    public static final int MAX_SHIELDS = 3;
    public static final int SHIELD_PRICE = 60;
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    public record EnergyState(int energy, int maxEnergy, long nextInSeconds) {
    }

    public record StreakState(int streakDays, int bestStreak, boolean playedToday, boolean atRisk, int shields) {
    }

    public record StreakUpdate(int streakDays, boolean increased, boolean shieldUsed) {
    }

    /** Tính lại thể lực theo thời gian đã trôi qua (ghi vào entity, service gọi save sau) */
    public EnergyState refresh(Student s) {
        LocalDateTime now = LocalDateTime.now(VN);
        int energy = s.getEnergy() == null ? MAX_ENERGY : s.getEnergy();
        LocalDateTime since = s.getEnergyUpdatedAt();
        if (energy >= MAX_ENERGY || since == null) {
            s.setEnergy(Math.min(Math.max(energy, 0), MAX_ENERGY));
            if (since == null) s.setEnergyUpdatedAt(now);
            return new EnergyState(s.getEnergy(), MAX_ENERGY, 0);
        }
        long minutes = Duration.between(since, now).toMinutes();
        int gained = (int) (minutes / REGEN_MINUTES);
        if (gained > 0) {
            energy = Math.min(MAX_ENERGY, energy + gained);
            since = energy >= MAX_ENERGY ? now : since.plusMinutes((long) gained * REGEN_MINUTES);
            s.setEnergy(energy);
            s.setEnergyUpdatedAt(since);
        }
        long next = energy >= MAX_ENERGY ? 0
                : Math.max(0, Duration.between(now, since.plusMinutes(REGEN_MINUTES)).getSeconds());
        return new EnergyState(energy, MAX_ENERGY, next);
    }

    /** Trừ 1 thể lực, hết thì báo lỗi 429 để FE hiện màn "nghỉ ngơi" */
    public EnergyState consume(Student s) {
        EnergyState st = refresh(s);
        if (st.energy() <= 0) {
            long min = Math.max(1, (st.nextInSeconds() + 59) / 60);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Hết thể lực rồi! Nghỉ ngơi " + min + " phút để hồi 1 năng lượng. Trong lúc chờ, em có thể ôn lý thuyết hoặc thảo luận với 3 AI (không tốn thể lực).");
        }
        if (st.energy() >= MAX_ENERGY) {
            // Bắt đầu đếm giờ hồi từ lúc dùng điểm đầu tiên
            s.setEnergyUpdatedAt(LocalDateTime.now(VN));
        }
        s.setEnergy(st.energy() - 1);
        return refresh(s);
    }

    /** Ghi nhận học sinh vừa học hôm nay */
    public StreakUpdate recordActivity(Student s) {
        LocalDate today = LocalDate.now(VN);
        LocalDate last = s.getLastActiveDate();
        int streak = s.getStreakDays() == null ? 0 : s.getStreakDays();
        boolean shieldUsed = false;
        if (last != null && last.equals(today)) {
            return new StreakUpdate(streak, false, false);
        }
        if (last != null && last.equals(today.minusDays(1))) {
            streak++;
        } else if (last != null && last.equals(today.minusDays(2)) && s.getStreakShields() > 0) {
            s.setStreakShields(s.getStreakShields() - 1);
            shieldUsed = true;
            streak++;
        } else {
            streak = 1;
        }
        s.setStreakDays(streak);
        s.setBestStreak(Math.max(s.getBestStreak() == null ? 0 : s.getBestStreak(), streak));
        s.setLastActiveDate(today);
        return new StreakUpdate(streak, true, shieldUsed);
    }

    /** Chuỗi hiển thị: lỡ quá 1 ngày (không có khiên) thì coi như đã đứt */
    public StreakState streak(Student s) {
        LocalDate today = LocalDate.now(VN);
        LocalDate last = s.getLastActiveDate();
        int streak = s.getStreakDays() == null ? 0 : s.getStreakDays();
        int shields = s.getStreakShields() == null ? 0 : s.getStreakShields();
        boolean playedToday = today.equals(last);
        boolean alive = last != null && (playedToday || last.equals(today.minusDays(1))
                || (last.equals(today.minusDays(2)) && shields > 0));
        int best = s.getBestStreak() == null ? 0 : s.getBestStreak();
        return new StreakState(alive ? streak : 0, best, playedToday, alive && !playedToday, shields);
    }

    public void buyShield(Student s) {
        if (s.getStreakShields() >= MAX_SHIELDS) {
            throw ApiException.badRequest("Em đang có đủ " + MAX_SHIELDS + " khiên rồi");
        }
        if (s.getCoins() < SHIELD_PRICE) {
            throw ApiException.badRequest("Cần " + SHIELD_PRICE + " xu để mua khiên giữ chuỗi");
        }
        s.setCoins(s.getCoins() - SHIELD_PRICE);
        s.setStreakShields(s.getStreakShields() + 1);
    }
}

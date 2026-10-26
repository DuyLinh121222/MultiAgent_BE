package com.mael.dto.game;

/** @param timeLeft số giây còn lại khi kết thúc (thưởng điểm nếu đúng hết) */
public record ArenaFinishRequest(Integer timeLeft) {
}

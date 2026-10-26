package com.mael.dto.game;

import java.util.List;

/**
 * Một lượt Đấu trường: tối đa 10 câu ngắn, có giờ và số tim.
 *
 * @param runId mã lượt chơi, gửi kèm khi trả lời từng câu và khi kết thúc
 */
public record ArenaDetail(
        String runId,
        String topicId,
        String name,
        int timeLimitSeconds,
        int hearts,
        int bestScore,
        List<StageQuestion> questions,
        GameProfile profile
) {
}

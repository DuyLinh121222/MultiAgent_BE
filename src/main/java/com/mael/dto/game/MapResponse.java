package com.mael.dto.game;

import java.util.List;

/**
 * Bản đồ ải theo khối lớp của học sinh.
 *
 * @param nextStageType LESSON | BOSS | null (đã phá đảo)
 * @param nextStageId   lessonId hoặc topicId của ải tiếp theo cần chơi
 */
public record MapResponse(
        GameProfile profile,
        Integer gradeLevel,
        String pathName,
        String pathDescription,
        int clearedStages,
        int totalStages,
        String nextStageType,
        String nextStageId,
        String nextStageTitle,
        List<ChapterNode> chapters
) {
}

package com.mael.dto.lecturer;

import com.mael.entity.enums.ReviewStatus;

import java.util.List;

/** Cây nội dung: Lớp → Chương → Bài (ải), kèm số câu hỏi và số học sinh đã chơi */
public record ContentTree(
        String pathId,
        String name,
        int gradeLevel,
        List<TopicNode> topics
) {
    public record TopicNode(
            String id,
            String name,
            String description,
            int orderIndex,
            int semester,
            long bossQuestions,
            List<LessonNode> lessons
    ) {
    }

    public record LessonNode(
            String id,
            String title,
            int orderIndex,
            ReviewStatus reviewStatus,
            long questionsPublished,
            long questionsPending,
            long questionsTotal,
            long players,
            Double correctRate
    ) {
    }
}

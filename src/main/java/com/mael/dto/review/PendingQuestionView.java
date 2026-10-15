package com.mael.dto.review;

import com.mael.entity.enums.ContentSource;
import com.mael.entity.enums.Difficulty;
import com.mael.entity.enums.QuestionType;
import com.mael.entity.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PendingQuestionView(
        String id,
        String content,
        QuestionType questionType,
        List<String> options,
        String correctAnswer,
        String explanation,
        Difficulty difficulty,
        ContentSource source,
        String topicName,
        ReviewStatus reviewStatus,
        LocalDateTime createdAt
) {
}

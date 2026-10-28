package com.mael.dto.lecturer;

import com.mael.entity.enums.ContentSource;
import com.mael.entity.enums.Difficulty;
import com.mael.entity.enums.QuestionType;
import com.mael.entity.enums.ReviewStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Câu hỏi trong ngân hàng câu hỏi, kèm thống kê trả lời */
public record QuestionAdminView(
        String id,
        String content,
        QuestionType questionType,
        List<String> options,
        String correctAnswer,
        String explanation,
        Difficulty difficulty,
        ContentSource source,
        String topicId,
        String topicName,
        int gradeLevel,
        String lessonId,
        String lessonTitle,
        ReviewStatus reviewStatus,
        int answeredCount,
        int wrongCount,
        Double wrongRate,
        String reviewedByName,
        LocalDateTime createdAt
) {
}

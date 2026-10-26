package com.mael.dto.document;

import java.time.LocalDateTime;

/**
 * Tài liệu PDF giảng viên đã tải lên và kết quả AI soạn từ tài liệu.
 *
 * @param aiStatus        NONE | PROCESSING | DONE | FAILED
 * @param pendingQuestions số câu hỏi AI soạn đang chờ duyệt
 */
public record DocumentView(
        String id,
        String fileName,
        String fileUrl,
        long fileSize,
        String lessonId,
        String lessonTitle,
        String lessonStatus,
        String topicId,
        String topicName,
        String aiStatus,
        String aiNote,
        long pendingQuestions,
        long publishedQuestions,
        LocalDateTime uploadedAt
) {
}

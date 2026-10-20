package com.mael.entity;

/**
 * Một lỗi sai hay gặp của bài học (lưu jsonb trong LESSON.misconceptions).
 *
 * @param wrong câu nói sai (bạn AI phản biện dùng để "cố ý sai")
 * @param why   vì sao học sinh hay nhầm
 * @param fix   phát biểu đúng / cách sửa
 */
public record Misconception(String wrong, String why, String fix) {
}

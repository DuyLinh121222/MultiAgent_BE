package com.mael.dto.lecturer;

/** Thống kê 1 ải: bao nhiêu học sinh đã chơi, tỉ lệ đúng trung bình (theo lần tốt nhất) */
public record LessonStat(
        String lessonId,
        String title,
        String topicName,
        int gradeLevel,
        long players,
        long attempts,
        double avgStars,
        Double correctRate
) {
}

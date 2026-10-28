package com.mael.dto.lecturer;

/** Thống kê theo chương: số học sinh còn yếu, độ thành thạo trung bình, Đấu trường */
public record TopicStat(
        String topicId,
        String name,
        int gradeLevel,
        long weakStudents,
        Double avgMastery,
        long arenaPlays,
        Double arenaAccuracy,
        long bossClears
) {
}

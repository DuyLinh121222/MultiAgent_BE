package com.mael.dto.lecturer;

import java.time.LocalDate;
import java.util.List;

/** Học sinh cần hỗ trợ: nhiều chương chưa vững, độ thành thạo thấp */
public record StudentAlert(
        String studentId,
        String fullName,
        int gradeLevel,
        long weakTopics,
        Double avgMastery,
        List<String> topicNames,
        LocalDate lastActiveDate,
        int streakDays
) {
}

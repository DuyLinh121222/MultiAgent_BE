package com.mael.dto.lecturer;

import java.util.List;

/** Phân tích học tập cho giảng viên (lọc theo khối lớp, null = tất cả) */
public record LearningAnalytics(
        Integer grade,
        Overview overview,
        List<TopicStat> topics,
        List<LessonStat> lessons,
        List<QuestionAdminView> hardestQuestions,
        List<StudentAlert> studentsNeedHelp
) {
    public record Overview(
            long students,
            long activeStudents7d,
            long stagePlays,
            long arenaPlays,
            Double avgCorrectRate
    ) {
    }
}

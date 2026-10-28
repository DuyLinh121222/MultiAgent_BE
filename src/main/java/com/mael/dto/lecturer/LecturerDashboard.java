package com.mael.dto.lecturer;

import java.util.List;

/** Trang tổng quan của giảng viên: việc cần làm + hiệu quả duyệt + điểm nóng học tập */
public record LecturerDashboard(
        String lecturerName,
        long pendingLessons,
        long pendingQuestions,
        long openReports,
        long processingDocuments,
        long myReviewsTotal,
        long myReviewsWeek,
        long myApproved,
        long myRejected,
        long myDocuments,
        long activeStudents7d,
        List<ReviewHistoryItem> recentReviews,
        List<LessonStat> hardestLessons,
        List<QuestionAdminView> hardestQuestions
) {
}

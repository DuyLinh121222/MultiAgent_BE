package com.mael.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bảng điều khiển quản trị: số liệu tổng, doanh thu 6 tháng, đăng ký mới 14 ngày, cơ cấu gói / khối lớp.
 */
public record AdminOverview(
        Stats stats,
        List<MonthRevenue> revenueByMonth,
        List<DayCount> signupsByDay,
        List<NameCount> activeByPlan,
        List<NameCount> studentsByGrade,
        List<RecentPayment> recentPayments,
        List<AuditLogView> recentActivity
) {
    public record Stats(
            long totalStudents,
            long newStudents7d,
            long activeToday,
            long totalLecturers,
            long lockedAccounts,
            long paidSubscribers,
            long trialSubscribers,
            BigDecimal revenueThisMonth,
            BigDecimal revenueLastMonth,
            BigDecimal revenueTotal,
            long pendingLessons,
            long pendingQuestions,
            long openReports,
            long newLeads
    ) {
    }

    public record MonthRevenue(String month, BigDecimal revenue, long orders) {
    }

    public record DayCount(LocalDate date, long count) {
    }

    public record NameCount(String name, long count) {
    }

    public record RecentPayment(String id, String studentName, String planName, BigDecimal amount, String status,
                                LocalDateTime createdAt) {
    }
}

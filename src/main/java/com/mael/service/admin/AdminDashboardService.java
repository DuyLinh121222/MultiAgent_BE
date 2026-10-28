package com.mael.service.admin;

import com.mael.dto.admin.AdminOverview;
import com.mael.dto.admin.AdminOverview.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * Số liệu tổng quan cho quản trị viên. Dùng SQL gộp (GROUP BY) cho nhanh, không tải từng bản ghi lên.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

    private final JdbcTemplate jdbc;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AdminOverview overview() {
        LocalDate today = LocalDate.now(VN);
        YearMonth thisMonth = YearMonth.from(today);

        Stats stats = new Stats(
                count("select count(*) from student"),
                count("select count(*) from student where created_at >= ?", today.minusDays(6).atStartOfDay()),
                count("select count(*) from student where last_active_date = ?", today),
                count("select count(*) from user_account where role = 'LECTURER'"),
                count("select count(*) from user_account where status = 'LOCKED'"),
                count("""
                        select count(distinct s.student_id) from subscription s join subscription_plan p on p.plan_id = s.plan_id
                        where s.status = 'ACTIVE' and s.end_date > now() and p.price > 0""" ),
                count("""
                        select count(distinct s.student_id) from subscription s join subscription_plan p on p.plan_id = s.plan_id
                        where s.status = 'ACTIVE' and s.end_date > now() and p.price = 0
                          and not exists (select 1 from subscription s2 join subscription_plan p2 on p2.plan_id = s2.plan_id
                                          where s2.student_id = s.student_id and s2.status = 'ACTIVE' and s2.end_date > now() and p2.price > 0)"""),
                revenue(thisMonth.atDay(1), thisMonth.plusMonths(1).atDay(1)),
                revenue(thisMonth.minusMonths(1).atDay(1), thisMonth.atDay(1)),
                jdbc.queryForObject("select coalesce(sum(amount), 0) from payment where status = 'SUCCESS'", BigDecimal.class),
                count("select count(*) from lesson where review_status = 'PENDING_REVIEW'"),
                count("select count(*) from question where review_status = 'PENDING_REVIEW'"),
                count("select count(*) from content_report where status in ('OPEN', 'IN_REVIEW')"),
                count("select count(*) from org_lead where status = 'NEW'"),
                count("select count(*) from payment where status = 'PENDING' and customer_paid_at is not null")
        );

        return new AdminOverview(stats, revenueByMonth(thisMonth), signupsByDay(today), activeByPlan(), studentsByGrade(),
                recentPayments(), auditService.recent());
    }

    /** Doanh thu 6 tháng gần nhất, tháng không có giao dịch = 0 */
    private List<MonthRevenue> revenueByMonth(YearMonth thisMonth) {
        YearMonth from = thisMonth.minusMonths(5);
        Map<String, MonthRevenue> byMonth = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            String key = from.plusMonths(i).toString();
            byMonth.put(key, new MonthRevenue(key, BigDecimal.ZERO, 0));
        }
        jdbc.query("""
                        select to_char(date_trunc('month', coalesce(paid_at, created_at)), 'YYYY-MM') m, sum(amount) total, count(*) c
                        from payment where status = 'SUCCESS' and coalesce(paid_at, created_at) >= ?
                        group by 1""",
                rs -> {
                    String key = rs.getString("m");
                    if (byMonth.containsKey(key)) {
                        byMonth.put(key, new MonthRevenue(key, rs.getBigDecimal("total"), rs.getLong("c")));
                    }
                },
                from.atDay(1).atStartOfDay());
        return new ArrayList<>(byMonth.values());
    }

    /** Học sinh đăng ký mới mỗi ngày trong 14 ngày gần nhất */
    private List<DayCount> signupsByDay(LocalDate today) {
        LocalDate from = today.minusDays(13);
        Map<LocalDate, Long> counts = new HashMap<>();
        jdbc.query("select cast(created_at as date) d, count(*) c from student where created_at >= ? group by 1",
                rs -> { counts.put(rs.getDate("d").toLocalDate(), rs.getLong("c")); },
                from.atStartOfDay());
        List<DayCount> result = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            LocalDate d = from.plusDays(i);
            result.add(new DayCount(d, counts.getOrDefault(d, 0L)));
        }
        return result;
    }

    private List<NameCount> activeByPlan() {
        return jdbc.query("""
                        select p.name, count(distinct s.student_id) c
                        from subscription s join subscription_plan p on p.plan_id = s.plan_id
                        where s.status = 'ACTIVE' and s.end_date > now()
                        group by p.name, p.sort_order order by p.sort_order""",
                (rs, i) -> new NameCount(rs.getString("name"), rs.getLong("c")));
    }

    private List<NameCount> studentsByGrade() {
        Map<Integer, Long> counts = new HashMap<>();
        jdbc.query("select grade_level g, count(*) c from student group by 1",
                rs -> { counts.put(rs.getInt("g"), rs.getLong("c")); });
        List<NameCount> result = new ArrayList<>();
        for (int g = 6; g <= 9; g++) result.add(new NameCount("Lớp " + g, counts.getOrDefault(g, 0L)));
        return result;
    }

    private List<RecentPayment> recentPayments() {
        return jdbc.query("""
                        select pm.payment_id, st.full_name, p.name plan_name, pm.amount,
                               case when pm.status = 'PENDING' and pm.customer_paid_at is not null then 'WAITING' else pm.status end status,
                               pm.created_at
                        from payment pm join subscription s on s.subscription_id = pm.subscription_id
                        join subscription_plan p on p.plan_id = s.plan_id join student st on st.student_id = s.student_id
                        order by pm.created_at desc limit 6""",
                (rs, i) -> new RecentPayment(rs.getString("payment_id"), rs.getString("full_name"), rs.getString("plan_name"),
                        rs.getBigDecimal("amount"), rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    private BigDecimal revenue(LocalDate from, LocalDate to) {
        return jdbc.queryForObject("""
                        select coalesce(sum(amount), 0) from payment
                        where status = 'SUCCESS' and coalesce(paid_at, created_at) >= ? and coalesce(paid_at, created_at) < ?""",
                BigDecimal.class, from.atStartOfDay(), to.atStartOfDay());
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }
}

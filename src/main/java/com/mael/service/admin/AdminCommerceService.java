package com.mael.service.admin;

import com.mael.dto.admin.*;
import com.mael.entity.SubscriptionPlan;
import com.mael.entity.enums.PaymentStatus;
import com.mael.exception.ApiException;
import com.mael.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * Gói học (thêm / sửa / ẩn) và lịch sử giao dịch thanh toán.
 */
@Service
@RequiredArgsConstructor
public class AdminCommerceService {

    private final JdbcTemplate jdbc;
    private final SubscriptionPlanRepository planRepository;
    private final AuditService auditService;

    // ------------------------------- Gói học -------------------------------

    @Transactional(readOnly = true)
    public List<PlanAdminView> plans() {
        Map<String, long[]> counts = new HashMap<>();
        Map<String, BigDecimal> revenue = new HashMap<>();
        jdbc.query("""
                select s.plan_id,
                       count(distinct s.student_id) filter (where s.status = 'ACTIVE' and s.end_date > now()) active_subs,
                       count(pm.payment_id) filter (where pm.status = 'SUCCESS') sold,
                       coalesce(sum(pm.amount) filter (where pm.status = 'SUCCESS'), 0) revenue
                from subscription s left join payment pm on pm.subscription_id = s.subscription_id
                group by s.plan_id""", rs -> {
            counts.put(rs.getString("plan_id"), new long[]{rs.getLong("active_subs"), rs.getLong("sold")});
            revenue.put(rs.getString("plan_id"), rs.getBigDecimal("revenue"));
        });
        return planRepository.findAll().stream()
                .sorted(Comparator.comparing(SubscriptionPlan::getSortOrder).thenComparing(SubscriptionPlan::getPrice))
                .map(p -> toView(p, counts.getOrDefault(p.getId(), new long[2]), revenue.getOrDefault(p.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Transactional
    public PlanAdminView createPlan(PlanRequest req) {
        if (jdbc.queryForObject("select count(*) from subscription_plan where lower(name) = lower(?)", Long.class, req.name().trim()) > 0) {
            throw ApiException.conflict("Đã có gói tên này");
        }
        SubscriptionPlan p = new SubscriptionPlan();
        apply(p, req);
        p = planRepository.save(p);
        auditService.log("CREATE_PLAN", "PLAN", p.getId(), "Tạo gói " + p.getName() + " giá " + money(p.getPrice()));
        return toView(p, new long[2], BigDecimal.ZERO);
    }

    @Transactional
    public PlanAdminView updatePlan(String id, PlanRequest req) {
        SubscriptionPlan p = planRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy gói"));
        if (jdbc.queryForObject("select count(*) from subscription_plan where lower(name) = lower(?) and plan_id <> ?",
                Long.class, req.name().trim(), id) > 0) {
            throw ApiException.conflict("Đã có gói tên này");
        }
        // Gói dùng thử (giá 0) là gói hệ thống tặng khi đăng ký: không cho đổi thành gói có phí và ngược lại
        if ((p.getPrice().signum() == 0) != (req.price().signum() == 0)) {
            throw ApiException.badRequest("Không đổi gói dùng thử (0đ) thành gói trả phí hoặc ngược lại, hãy tạo gói mới");
        }
        String before = p.getName() + " " + money(p.getPrice()) + "/" + p.getDurationDays() + " ngày";
        apply(p, req);
        planRepository.flush();
        auditService.log("UPDATE_PLAN", "PLAN", id, "Sửa gói " + before + " → "
                + p.getName() + " " + money(p.getPrice()) + "/" + p.getDurationDays() + " ngày"
                + (Boolean.TRUE.equals(p.getActive()) ? "" : " (đang ẩn)"));
        return plans().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    /** Ẩn / hiện gói. Không xoá hẳn vì còn lịch sử thanh toán của học sinh. */
    @Transactional
    public PlanAdminView setPlanActive(String id, boolean active) {
        SubscriptionPlan p = planRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy gói"));
        if (!active && p.getPrice().signum() == 0
                && planRepository.findByActiveTrueOrderByPriceAsc().stream().filter(x -> x.getPrice().signum() == 0).count() <= 1) {
            throw ApiException.badRequest("Phải còn 1 gói dùng thử đang bật để tặng học sinh mới đăng ký");
        }
        p.setActive(active);
        planRepository.flush();
        auditService.log(active ? "SHOW_PLAN" : "HIDE_PLAN", "PLAN", id, (active ? "Hiện" : "Ẩn") + " gói " + p.getName());
        return plans().stream().filter(x -> x.id().equals(id)).findFirst().orElseThrow();
    }

    static String money(BigDecimal v) {
        return String.format(java.util.Locale.ROOT, "%,d", v.longValue()).replace(',', '.') + "đ";
    }

    private static void apply(SubscriptionPlan p, PlanRequest r) {
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setDurationDays(r.durationDays());
        p.setMaxAiSessions(r.maxAiSessions());
        p.setAudience(r.audience());
        p.setPriceUnit(r.priceUnit());
        p.setMinSeats(r.minSeats());
        p.setFeatures(r.features());
        p.setFeatured(Boolean.TRUE.equals(r.featured()));
        p.setSortOrder(r.sortOrder() == null ? 0 : r.sortOrder());
        p.setActive(r.active() == null || r.active());
    }

    private static PlanAdminView toView(SubscriptionPlan p, long[] counts, BigDecimal revenue) {
        return new PlanAdminView(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getDurationDays(),
                p.getMaxAiSessions(), p.getAudience(), p.getPriceUnit(), p.getMinSeats(), p.getFeatures(),
                Boolean.TRUE.equals(p.getFeatured()), p.getSortOrder() == null ? 0 : p.getSortOrder(),
                Boolean.TRUE.equals(p.getActive()), counts[0], counts[1], revenue);
    }

    // ------------------------------- Giao dịch -------------------------------

    @Transactional(readOnly = true)
    public PaymentSummary payments(PaymentStatus status, String q, int page, int size) {
        String from = """
                from payment pm join subscription s on s.subscription_id = pm.subscription_id
                join subscription_plan p on p.plan_id = s.plan_id
                join student st on st.student_id = s.student_id
                join user_account u on u.user_id = st.user_id
                where 1 = 1""";
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            where.append(" and (lower(st.full_name) like ? or lower(u.email) like ? or pm.transaction_code like ? or pm.payment_id like ?)");
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(like); args.add(like); args.add("%" + q.trim() + "%"); args.add(q.trim() + "%");
        }
        // Thẻ tổng hợp tính theo ô tìm kiếm, không theo trạng thái (để thấy được cả 3 con số)
        Map<String, Object> agg = jdbc.queryForMap("""
                select count(*) filter (where pm.status = 'SUCCESS') ok,
                       coalesce(sum(pm.amount) filter (where pm.status = 'SUCCESS'), 0) ok_amount,
                       count(*) filter (where pm.status = 'PENDING') pending,
                       count(*) filter (where pm.status = 'FAILED') failed
                """ + from + where, args.toArray());

        if (status != null) {
            where.append(" and pm.status = ?");
            args.add(status.name());
        }
        Long total = jdbc.queryForObject("select count(*) " + from + where, Long.class, args.toArray());
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add(safePage * safeSize);
        List<AdminPaymentRow> rows = jdbc.query("""
                select pm.payment_id, u.user_id, st.full_name, u.email, p.name plan_name, pm.amount, pm.method, pm.status,
                       pm.transaction_code, pm.created_at, pm.paid_at
                """ + from + where + " order by pm.created_at desc limit ? offset ?", (rs, i) -> {
            Timestamp paid = rs.getTimestamp("paid_at");
            return new AdminPaymentRow(rs.getString("payment_id"), rs.getString("user_id"), rs.getString("full_name"),
                    rs.getString("email"), rs.getString("plan_name"), rs.getBigDecimal("amount"), rs.getString("method"),
                    rs.getString("status"), rs.getString("transaction_code"),
                    rs.getTimestamp("created_at").toLocalDateTime(), paid == null ? null : paid.toLocalDateTime());
        }, pageArgs.toArray());

        return new PaymentSummary(((Number) agg.get("ok")).longValue(), (BigDecimal) agg.get("ok_amount"),
                ((Number) agg.get("pending")).longValue(), ((Number) agg.get("failed")).longValue(),
                new PageResponse<>(rows, total == null ? 0 : total, safePage, safeSize));
    }
}

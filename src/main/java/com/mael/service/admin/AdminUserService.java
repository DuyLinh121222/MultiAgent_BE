package com.mael.service.admin;

import com.mael.dto.admin.*;
import com.mael.entity.*;
import com.mael.entity.enums.*;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import com.mael.service.DtoMapper;
import com.mael.service.NotificationService;
import com.mael.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Quản lý tài khoản: xem / tìm kiếm, tạo giảng viên & quản trị viên, khoá / mở khoá,
 * cấp lại mật khẩu, sửa hồ sơ, kích hoạt gói thủ công cho học sinh.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final UserAccountRepository userAccountRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final WeaknessRepository weaknessRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    /** Gói đang có hiệu lực của học sinh (lấy gói hết hạn muộn nhất) */
    private static final String BASE_SELECT = """
            select u.user_id, u.email, u.role, u.status, u.created_at, u.last_login_at,
                   coalesce(st.full_name, l.full_name, u.display_name, u.email) full_name,
                   st.grade_level, st.phone, st.xp, st.streak_days, st.last_active_date, l.specialization,
                   cur.plan_name, cur.end_date, cur.price
            from user_account u
            left join student st on st.user_id = u.user_id
            left join lecturer l on l.user_id = u.user_id
            left join lateral (
                select p.name plan_name, s.end_date, p.price
                from subscription s join subscription_plan p on p.plan_id = s.plan_id
                where s.student_id = st.student_id and s.status = 'ACTIVE' and s.end_date > now()
                order by s.end_date desc limit 1
            ) cur on true
            """;

    private static final RowMapper<AdminUserRow> ROW = (rs, i) -> {
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        Timestamp end = rs.getTimestamp("end_date");
        Date lastActive = rs.getDate("last_active_date");
        java.math.BigDecimal price = rs.getBigDecimal("price");
        return new AdminUserRow(
                rs.getString("user_id"), rs.getString("email"),
                Role.valueOf(rs.getString("role")), AccountStatus.valueOf(rs.getString("status")),
                rs.getString("full_name"),
                (Integer) rs.getObject("grade_level"), rs.getString("phone"),
                (Integer) rs.getObject("xp"), (Integer) rs.getObject("streak_days"),
                lastActive == null ? null : lastActive.toLocalDate(),
                rs.getString("plan_name"), end == null ? null : end.toLocalDateTime(),
                price == null ? null : price.signum() == 0,
                rs.getString("specialization"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                lastLogin == null ? null : lastLogin.toLocalDateTime());
    };

    /**
     * Danh sách tài khoản có lọc + phân trang.
     *
     * @param plan chỉ áp dụng cho học sinh: PAID (đang dùng gói trả phí), TRIAL (đang dùng thử), NONE (hết hạn)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminUserRow> list(Role role, AccountStatus status, Integer grade, String plan, String q,
                                           int page, int size) {
        StringBuilder where = new StringBuilder(" where 1 = 1");
        List<Object> args = new ArrayList<>();
        if (role != null) { where.append(" and u.role = ?"); args.add(role.name()); }
        if (status != null) { where.append(" and u.status = ?"); args.add(status.name()); }
        if (grade != null) { where.append(" and st.grade_level = ?"); args.add(grade); }
        if (plan != null && !plan.isBlank()) {
            switch (plan.toUpperCase(Locale.ROOT)) {
                case "PAID" -> where.append(" and cur.price > 0");
                case "TRIAL" -> where.append(" and cur.price = 0");
                case "NONE" -> where.append(" and st.student_id is not null and cur.plan_name is null");
                default -> throw ApiException.badRequest("Bộ lọc gói không hợp lệ");
            }
        }
        if (q != null && !q.isBlank()) {
            where.append(" and (lower(u.email) like ? or lower(coalesce(st.full_name, l.full_name, u.display_name, '')) like ? or st.phone like ?)");
            String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            args.add(like); args.add(like); args.add("%" + q.trim() + "%");
        }
        Long total = jdbc.queryForObject("select count(*) from (" + BASE_SELECT + where + ") x", Long.class, args.toArray());

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add(safePage * safeSize);
        List<AdminUserRow> items = jdbc.query(BASE_SELECT + where + " order by u.created_at desc limit ? offset ?",
                ROW, pageArgs.toArray());
        return new PageResponse<>(items, total == null ? 0 : total, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public AdminUserDetail detail(String userId) {
        AdminUserRow row = row(userId);
        AdminUserDetail.StudentInfo studentInfo = null;
        AdminUserDetail.LecturerInfo lecturerInfo = null;

        if (row.role() == Role.STUDENT) {
            Student s = studentRepository.findByUserId(userId).orElseThrow(() -> ApiException.notFound("Không tìm thấy hồ sơ học sinh"));
            String sid = s.getId();
            studentInfo = new AdminUserDetail.StudentInfo(
                    sid, s.getDateOfBirth(), s.getLearningGoal(), Boolean.TRUE.equals(s.getOnboarded()),
                    s.getCoins(), s.getXp(), s.getXp() / 100 + 1, s.getStreakDays(), s.getBestStreak(),
                    count("select count(*) from stage_result where student_id = ? and stars > 0", sid),
                    count("select coalesce(sum(stars), 0) from stage_result where student_id = ?", sid),
                    count("select count(*) from arena_play where student_id = ?", sid),
                    count("select count(*) from discussion_session where student_id = ?", sid),
                    subscriptionService.historyOf(sid),
                    paymentRepository.findByStudentId(sid).stream().map(p -> DtoMapper.toPaymentView(p, null)).toList(),
                    weaknessRepository.findByStudentId(sid).stream().map(DtoMapper::toWeaknessView).toList());
        }
        // Admin có thể có hồ sơ giảng viên tự tạo khi duyệt bài, nên lấy theo user_id cho cả 2 role
        if (row.role() != Role.STUDENT) {
            Lecturer l = lecturerRepository.findByUserId(userId).orElse(null);
            if (l != null) {
                String lid = l.getId();
                Timestamp last = jdbc.queryForObject("select max(reviewed_at) from content_review where lecturer_id = ?",
                        Timestamp.class, lid);
                lecturerInfo = new AdminUserDetail.LecturerInfo(lid, l.getSpecialization(), l.getAcademicDegree(), l.getWorkplace(),
                        count("select count(*) from content_review where lecturer_id = ?", lid),
                        count("select count(*) from content_review where lecturer_id = ? and decision = 'APPROVED'", lid),
                        count("select count(*) from content_review where lecturer_id = ? and decision = 'REJECTED'", lid),
                        count("select count(*) from lesson_material where uploaded_by = ?", lid),
                        count("select count(*) from content_report where handled_by = ?", lid),
                        last == null ? null : last.toLocalDateTime());
            }
        }
        return new AdminUserDetail(row, studentInfo, lecturerInfo);
    }

    /** Tạo tài khoản giảng viên / quản trị viên. Học sinh tự đăng ký nên không tạo ở đây. */
    @Transactional
    public AccountCredential createStaff(CreateStaffRequest req) {
        if (req.role() == Role.STUDENT) {
            throw ApiException.badRequest("Học sinh tự đăng ký trên trang chủ, admin chỉ tạo giảng viên hoặc quản trị viên");
        }
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        String password = req.password() == null || req.password().isBlank() ? randomPassword() : req.password();
        UserAccount account = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(req.role())
                .displayName(req.role() == Role.ADMIN ? req.fullName().trim() : null)
                .build());
        if (req.role() == Role.LECTURER) {
            lecturerRepository.save(Lecturer.builder()
                    .user(account)
                    .fullName(req.fullName().trim())
                    .specialization(blankToNull(req.specialization()))
                    .academicDegree(blankToNull(req.academicDegree()))
                    .workplace(blankToNull(req.workplace()))
                    .build());
        }
        auditService.log("CREATE_ACCOUNT", "USER", account.getId(),
                "Tạo tài khoản " + roleName(req.role()) + " " + req.fullName().trim() + " (" + email + ")");
        return new AccountCredential(account.getId(), email, password);
    }

    @Transactional
    public AdminUserRow update(String userId, UpdateUserRequest req) {
        UserAccount account = account(userId);
        switch (account.getRole()) {
            case STUDENT -> {
                Student s = studentRepository.findByUserId(userId).orElseThrow();
                if (notBlank(req.fullName())) s.setFullName(req.fullName().trim());
                if (req.phone() != null) s.setPhone(blankToNull(req.phone()));
                if (req.gradeLevel() != null) s.setGradeLevel(req.gradeLevel());
            }
            case LECTURER -> {
                Lecturer l = lecturerRepository.findByUserId(userId).orElseThrow();
                if (notBlank(req.fullName())) l.setFullName(req.fullName().trim());
                if (req.specialization() != null) l.setSpecialization(blankToNull(req.specialization()));
                if (req.academicDegree() != null) l.setAcademicDegree(blankToNull(req.academicDegree()));
                if (req.workplace() != null) l.setWorkplace(blankToNull(req.workplace()));
            }
            case ADMIN -> {
                if (notBlank(req.fullName())) account.setDisplayName(req.fullName().trim());
            }
        }
        auditService.log("UPDATE_ACCOUNT", "USER", userId, "Sửa hồ sơ " + account.getEmail());
        // flush để câu SQL đọc lại thấy dữ liệu mới
        userAccountRepository.flush();
        return row(userId);
    }

    /** Khoá / mở khoá. Tài khoản bị khoá không đăng nhập được và token cũ hết tác dụng ngay. */
    @Transactional
    public AdminUserRow setStatus(String userId, StatusRequest req) {
        UserAccount account = account(userId);
        if (req.status() == AccountStatus.LOCKED) {
            if (userId.equals(currentUser.userId())) {
                throw ApiException.badRequest("Bạn không thể tự khoá tài khoản của mình");
            }
            if (account.getRole() == Role.ADMIN && account.getStatus() == AccountStatus.ACTIVE
                    && count("select count(*) from user_account where role = 'ADMIN' and status = 'ACTIVE'") <= 1) {
                throw ApiException.badRequest("Phải còn ít nhất 1 quản trị viên đang hoạt động");
            }
        }
        if (account.getStatus() != req.status()) {
            account.setStatus(req.status());
            String reason = notBlank(req.reason()) ? ". Lý do: " + req.reason().trim() : "";
            auditService.log(req.status() == AccountStatus.LOCKED ? "LOCK_ACCOUNT" : "UNLOCK_ACCOUNT", "USER", userId,
                    (req.status() == AccountStatus.LOCKED ? "Khoá " : "Mở khoá ") + account.getEmail() + reason);
        }
        userAccountRepository.flush();
        return row(userId);
    }

    @Transactional
    public AccountCredential resetPassword(String userId) {
        UserAccount account = account(userId);
        String password = randomPassword();
        account.setPasswordHash(passwordEncoder.encode(password));
        auditService.log("RESET_PASSWORD", "USER", userId, "Cấp lại mật khẩu cho " + account.getEmail());
        return new AccountCredential(userId, account.getEmail(), password);
    }

    /**
     * Kích hoạt gói cho học sinh không qua MoMo (học sinh của trung tâm / trường đã ký hợp đồng, quà tặng, bù lỗi thanh toán).
     * Đang còn hạn thì cộng nối tiếp như khi mua.
     */
    @Transactional
    public AdminUserDetail grantPlan(String userId, GrantPlanRequest req) {
        UserAccount account = account(userId);
        if (account.getRole() != Role.STUDENT) throw ApiException.badRequest("Chỉ kích hoạt gói cho học sinh");
        Student student = studentRepository.findByUserId(userId).orElseThrow();
        SubscriptionPlan plan = planRepository.findById(req.planId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy gói"));
        if (plan.getPrice().signum() == 0) throw ApiException.badRequest("Không tặng gói dùng thử, chọn gói trả phí");
        Subscription sub = subscriptionRepository.save(Subscription.builder()
                .student(student).plan(plan).status(SubscriptionStatus.PENDING).build());
        subscriptionService.activate(sub);
        notificationService.notify(student, NotificationType.SYSTEM, "Gói học đã được kích hoạt",
                "MAEL đã kích hoạt " + plan.getName() + " cho bạn. Chúc bạn học vui!", null);
        String note = notBlank(req.note()) ? ". Ghi chú: " + req.note().trim() : "";
        auditService.log("GRANT_PLAN", "USER", userId,
                "Kích hoạt " + plan.getName() + " cho " + student.getFullName() + " (" + account.getEmail() + ")" + note);
        subscriptionRepository.flush();
        return detail(userId);
    }

    // ------------------------------------------------------------------

    private AdminUserRow row(String userId) {
        List<AdminUserRow> rows = jdbc.query(BASE_SELECT + " where u.user_id = ?", ROW, userId);
        if (rows.isEmpty()) throw ApiException.notFound("Không tìm thấy tài khoản");
        return rows.get(0);
    }

    private UserAccount account(String userId) {
        return userAccountRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }

    static String randomPassword() {
        StringBuilder sb = new StringBuilder("Mael@");
        for (int i = 0; i < 6; i++) sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        return sb.toString();
    }

    private static String roleName(Role role) {
        return switch (role) {
            case ADMIN -> "quản trị viên";
            case LECTURER -> "giảng viên";
            case STUDENT -> "học sinh";
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

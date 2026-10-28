package com.mael.controller;

import com.mael.dto.admin.*;
import com.mael.dto.review.CreateLessonRequest;
import com.mael.dto.review.CreateQuestionRequest;
import com.mael.entity.enums.AccountStatus;
import com.mael.entity.enums.Role;
import com.mael.service.AdminContentService;
import com.mael.service.admin.AdminCommerceService;
import com.mael.service.admin.AdminDashboardService;
import com.mael.service.admin.AdminUserService;
import com.mael.service.admin.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Cổng quản trị (chỉ role ADMIN, chặn ở SecurityConfig): tổng quan, người dùng, gói học, giao dịch, nhật ký.
 * Yêu cầu tư vấn trung tâm / trường học ở LeadController (/api/admin/leads).
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminContentService adminContentService;
    private final AdminDashboardService dashboardService;
    private final AdminUserService userService;
    private final AdminCommerceService commerceService;
    private final AuditService auditService;

    // ---------------- Tổng quan ----------------

    @GetMapping("/overview")
    public AdminOverview overview() {
        return dashboardService.overview();
    }

    // ---------------- Người dùng ----------------

    @GetMapping("/users")
    public PageResponse<AdminUserRow> users(@RequestParam(required = false) Role role,
                                            @RequestParam(required = false) AccountStatus status,
                                            @RequestParam(required = false) Integer grade,
                                            @RequestParam(required = false) String plan,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return userService.list(role, status, grade, plan, q, page, size);
    }

    @GetMapping("/users/{userId}")
    public AdminUserDetail user(@PathVariable String userId) {
        return userService.detail(userId);
    }

    /** Tạo tài khoản giảng viên / quản trị viên, trả về mật khẩu tạm */
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountCredential createStaff(@Valid @RequestBody CreateStaffRequest req) {
        return userService.createStaff(req);
    }

    @PutMapping("/users/{userId}")
    public AdminUserRow updateUser(@PathVariable String userId, @Valid @RequestBody UpdateUserRequest req) {
        return userService.update(userId, req);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserRow setStatus(@PathVariable String userId, @Valid @RequestBody StatusRequest req) {
        return userService.setStatus(userId, req);
    }

    @PostMapping("/users/{userId}/reset-password")
    public AccountCredential resetPassword(@PathVariable String userId) {
        return userService.resetPassword(userId);
    }

    @PostMapping("/users/{userId}/grant-plan")
    public AdminUserDetail grantPlan(@PathVariable String userId, @Valid @RequestBody GrantPlanRequest req) {
        return userService.grantPlan(userId, req);
    }

    /** Nạp (số dương) / trừ (số âm) xu cho học sinh */
    @PostMapping("/users/{userId}/coins")
    public AdminUserDetail adjustCoins(@PathVariable String userId, @Valid @RequestBody CoinAdjustRequest req) {
        return userService.adjustCoins(userId, req);
    }

    // ---------------- Gói học & giao dịch ----------------

    @GetMapping("/plans")
    public List<PlanAdminView> plans() {
        return commerceService.plans();
    }

    @PostMapping("/plans")
    @ResponseStatus(HttpStatus.CREATED)
    public PlanAdminView createPlan(@Valid @RequestBody PlanRequest req) {
        return commerceService.createPlan(req);
    }

    @PutMapping("/plans/{planId}")
    public PlanAdminView updatePlan(@PathVariable String planId, @Valid @RequestBody PlanRequest req) {
        return commerceService.updatePlan(planId, req);
    }

    @PatchMapping("/plans/{planId}/active")
    public PlanAdminView setPlanActive(@PathVariable String planId, @RequestParam boolean active) {
        return commerceService.setPlanActive(planId, active);
    }

    @GetMapping("/payments")
    /** status: WAITING (học sinh báo đã chuyển, chờ xác nhận), PENDING, SUCCESS, FAILED, REFUNDED */
    public PaymentSummary payments(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return commerceService.payments(status, q, page, size);
    }

    /** Đã thấy tiền về ví MoMo: kích hoạt gói cho học sinh */
    @PostMapping("/payments/{paymentId}/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmPayment(@PathVariable String paymentId, @Valid @RequestBody(required = false) PaymentDecisionRequest req) {
        commerceService.confirmPayment(paymentId, req == null ? null : req.note());
    }

    /** Không thấy tiền / sai số tiền: từ chối, note = lý do (bắt buộc) */
    @PostMapping("/payments/{paymentId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectPayment(@PathVariable String paymentId, @Valid @RequestBody PaymentDecisionRequest req) {
        commerceService.rejectPayment(paymentId, req.note());
    }

    // ---------------- Nhật ký ----------------

    @GetMapping("/audit-logs")
    public PageResponse<AuditLogView> auditLogs(@RequestParam(required = false) String action,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "30") int size) {
        return auditService.list(action, page, size);
    }

    // ---------------- Soạn nội dung gửi giảng viên duyệt ----------------

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createLesson(@Valid @RequestBody CreateLessonRequest req) {
        return adminContentService.createLesson(req);
    }

    @PostMapping("/lessons/{lessonId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitLesson(@PathVariable String lessonId) {
        adminContentService.submitLessonForReview(lessonId);
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> createQuestion(@Valid @RequestBody CreateQuestionRequest req) {
        return adminContentService.createQuestion(req);
    }

    @PostMapping("/questions/{questionId}/submit")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void submitQuestion(@PathVariable String questionId) {
        adminContentService.submitQuestionForReview(questionId);
    }
}

package com.mael.service;

import com.mael.dto.plan.PlanView;
import com.mael.dto.plan.SubscriptionView;
import com.mael.entity.Student;
import com.mael.entity.Subscription;
import com.mael.entity.SubscriptionPlan;
import com.mael.entity.enums.SubscriptionStatus;
import com.mael.exception.ApiException;
import com.mael.repository.DiscussionSessionRepository;
import com.mael.repository.SubscriptionPlanRepository;
import com.mael.repository.SubscriptionRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final DiscussionSessionRepository sessionRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<PlanView> activePlans() {
        return planRepository.findByActiveTrueOrderBySortOrderAscPriceAsc().stream().map(DtoMapper::toPlanView).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> currentSubscription(String studentId) {
        return subscriptionRepository.findCurrentActive(studentId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public SubscriptionView mySubscription() {
        return currentSubscription(currentUser.student().getId())
                .map(DtoMapper::toSubscriptionView)
                .orElse(null);
    }

    /** Lịch sử gói đã kích hoạt (mới nhất trước), bỏ các đơn chưa thanh toán */
    @Transactional(readOnly = true)
    public List<SubscriptionView> historyOf(String studentId) {
        return subscriptionRepository.findByStudentId(studentId).stream()
                .filter(x -> x.getStatus() != SubscriptionStatus.PENDING && x.getStatus() != SubscriptionStatus.CANCELLED)
                .sorted(java.util.Comparator.comparing(Subscription::getCreatedAt, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
                .map(DtoMapper::toSubscriptionView).toList();
    }

    /**
     * Gói hiển thị: khách xem tất cả (học sinh, trung tâm, trường học);
     * học sinh đã đăng nhập chỉ thấy gói dành cho học sinh, bỏ gói dùng thử đã dùng.
     */
    @Transactional(readOnly = true)
    public List<PlanView> plansFor(Student student) {
        if (student == null) return activePlans();
        boolean usedTrial = subscriptionRepository.existsByStudentIdAndPlanPrice(student.getId(), BigDecimal.ZERO);
        return planRepository.findByActiveTrueOrderBySortOrderAscPriceAsc().stream()
                .filter(p -> p.getAudience() == com.mael.entity.enums.PlanAudience.STUDENT)
                .filter(p -> !(usedTrial && p.getPrice().signum() == 0))
                .map(DtoMapper::toPlanView).toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionView> myHistory() {
        return subscriptionRepository.findByStudentId(currentUser.student().getId()).stream()
                .map(DtoMapper::toSubscriptionView).toList();
    }

    /**
     * Tạo gói dùng thử 7 ngày cho học sinh mới (mỗi học sinh chỉ 1 lần).
     */
    @Transactional
    public Subscription startTrial(Student student) {
        SubscriptionPlan trial = planRepository.findFirstByActiveTrueAndPrice(BigDecimal.ZERO)
                .orElseThrow(() -> ApiException.badRequest("Chưa cấu hình gói dùng thử"));
        if (subscriptionRepository.existsByStudentIdAndPlanPrice(student.getId(), BigDecimal.ZERO)) {
            throw ApiException.conflict("Bạn đã dùng thử rồi");
        }
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepository.save(Subscription.builder()
                .student(student)
                .plan(trial)
                .startDate(now)
                .endDate(now.plusDays(trial.getDurationDays()))
                .status(SubscriptionStatus.ACTIVE)
                .build());
    }

    /**
     * Kích hoạt gói sau khi thanh toán thành công. Nếu đang còn hạn thì cộng nối tiếp.
     */
    @Transactional
    public void activate(Subscription subscription) {
        LocalDateTime start = currentSubscription(subscription.getStudent().getId())
                .map(Subscription::getEndDate)
                .filter(end -> end.isAfter(LocalDateTime.now()))
                .orElse(LocalDateTime.now());
        subscription.setStartDate(start);
        subscription.setEndDate(start.plusDays(subscription.getPlan().getDurationDays()));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
    }

    /**
     * Chặn khi hết hạn dùng thử / gói. Trả về gói đang dùng.
     */
    @Transactional(readOnly = true)
    public Subscription requireActive(Student student) {
        return currentSubscription(student.getId())
                .orElseThrow(() -> ApiException.paymentRequired(
                        "Gói học của bạn đã hết hạn. Kích hoạt gói 99.000đ/tháng để tiếp tục đi ải và học cùng 3 AI."));
    }

    /**
     * Kiểm tra số phiên thảo luận AI theo SUBSCRIPTION_PLAN.max_ai_sessions (null = không giới hạn).
     */
    @Transactional(readOnly = true)
    public void checkAiSessionQuota(Student student) {
        Subscription sub = requireActive(student);
        Integer max = sub.getPlan().getMaxAiSessions();
        if (max != null && sessionRepository.countByStudentIdAndStartedAtAfter(student.getId(), sub.getStartDate()) >= max) {
            throw ApiException.paymentRequired("Bạn đã dùng hết " + max + " phiên thảo luận AI của gói hiện tại.");
        }
    }
}

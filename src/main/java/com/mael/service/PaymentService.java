package com.mael.service;

import com.mael.dto.payment.CreatePaymentRequest;
import com.mael.dto.payment.PaymentView;
import com.mael.entity.Payment;
import com.mael.entity.Student;
import com.mael.entity.Subscription;
import com.mael.entity.SubscriptionPlan;
import com.mael.entity.enums.PlanAudience;
import com.mael.entity.enums.NotificationType;
import com.mael.entity.enums.PaymentStatus;
import com.mael.entity.enums.SubscriptionStatus;
import com.mael.exception.ApiException;
import com.mael.repository.PaymentRepository;
import com.mael.repository.SubscriptionPlanRepository;
import com.mael.repository.SubscriptionRepository;
import com.mael.security.CurrentUser;
import com.mael.service.payment.PaymentGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tạo đơn thanh toán gói học. Trạng thái chỉ chuyển SUCCESS khi nhận IPN hợp lệ
 * (hiện tại: hoặc gọi mock-confirm khi app.payment.mock-confirm-enabled=true).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;
    private final PaymentGateway paymentGateway;
    private final CurrentUser currentUser;

    @Value("${app.payment.mock-confirm-enabled:false}")
    private boolean mockConfirmEnabled;

    @Transactional
    public PaymentView create(CreatePaymentRequest req) {
        Student student = currentUser.student();
        SubscriptionPlan plan = planRepository.findById(req.planId())
                .filter(SubscriptionPlan::getActive)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy gói học"));
        if (plan.getAudience() != PlanAudience.STUDENT) {
            throw ApiException.badRequest("Gói " + plan.getName() + " dành cho tổ chức. Vui lòng gửi yêu cầu tư vấn để được báo giá.");
        }
        if (DtoMapper.isTrial(plan)) {
            throw ApiException.badRequest("Gói dùng thử được kích hoạt tự động khi đăng ký");
        }

        Subscription subscription = subscriptionRepository.save(Subscription.builder()
                .student(student)
                .plan(plan)
                .status(SubscriptionStatus.PENDING)
                .build());
        Payment payment = paymentRepository.save(Payment.builder()
                .subscription(subscription)
                .amount(plan.getPrice())
                .method("MOMO_WALLET")
                .requestId(UUID.randomUUID().toString())
                .status(PaymentStatus.PENDING)
                .build());

        PaymentGateway.PaymentInit init = paymentGateway.createPayment(payment,
                "Thanh toán " + plan.getName() + " - MAEL");
        payment.setPayUrl(init.payUrl());
        payment.setResultCode(init.resultCode());
        return DtoMapper.toPaymentView(payment, init.qrCodeUrl());
    }

    @Transactional(readOnly = true)
    public PaymentView get(String paymentId) {
        return DtoMapper.toPaymentView(getOwnPayment(paymentId), null);
    }

    @Transactional(readOnly = true)
    public List<PaymentView> myPayments() {
        return paymentRepository.findByStudentId(currentUser.student().getId()).stream()
                .map(p -> DtoMapper.toPaymentView(p, null)).toList();
    }

    /**
     * CHỈ DÙNG KHI DEV: giả lập MoMo báo thanh toán thành công.
     * Tắt bằng app.payment.mock-confirm-enabled=false khi đã tích hợp MoMo thật.
     */
    @Transactional
    public PaymentView mockConfirm(String paymentId) {
        if (!mockConfirmEnabled) {
            throw ApiException.forbidden("Chức năng giả lập thanh toán đã tắt");
        }
        Payment payment = getOwnPayment(paymentId);
        markSuccess(payment, "MOCK-" + System.currentTimeMillis());
        return DtoMapper.toPaymentView(payment, null);
    }

    /**
     * Đánh dấu thanh toán thành công + kích hoạt gói. Idempotent: chỉ xử lý khi còn PENDING.
     * MoMo IPN handler sau này gọi hàm này sau khi đã kiểm chữ ký.
     */
    @Transactional
    public void markSuccess(Payment payment, String transactionCode) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return;
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionCode(transactionCode);
        payment.setResultCode(0);
        payment.setPaidAt(LocalDateTime.now());
        subscriptionService.activate(payment.getSubscription());

        Subscription sub = payment.getSubscription();
        notificationService.notify(sub.getStudent(), NotificationType.PAYMENT,
                "Thanh toán thành công " + sub.getPlan().getName(),
                "Gói học đã được kích hoạt đến " + sub.getEndDate().toLocalDate() + ". Chúc bạn học vui!",
                payment);
    }

    private Payment getOwnPayment(String paymentId) {
        String studentId = currentUser.student().getId();
        return paymentRepository.findDetailById(paymentId)
                .filter(p -> p.getSubscription().getStudent().getId().equals(studentId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy giao dịch"));
    }
}

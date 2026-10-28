package com.mael.service;

import com.mael.dto.common.NotificationView;
import com.mael.dto.discussion.AgentView;
import com.mael.dto.discussion.MessageView;
import com.mael.dto.payment.PaymentView;
import com.mael.dto.photo.PhotoProblemView;
import com.mael.dto.photo.SolutionView;
import com.mael.dto.plan.PlanView;
import com.mael.dto.plan.SubscriptionView;
import com.mael.dto.quiz.WeaknessView;
import com.mael.entity.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Chuyển entity sang DTO. Gọi bên trong transaction để tránh lỗi lazy loading.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static PlanView toPlanView(SubscriptionPlan p) {
        List<String> features = p.getFeatures() == null ? List.of()
                : Arrays.stream(p.getFeatures().split("\\R")).map(String::trim).filter(x -> !x.isEmpty()).toList();
        return new PlanView(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getDurationDays(), p.getMaxAiSessions(), isTrial(p), p.getAudience(), p.getPriceUnit(),
                p.getMinSeats(), features, Boolean.TRUE.equals(p.getFeatured()));
    }

    public static boolean isTrial(SubscriptionPlan p) {
        return p.getPrice().compareTo(BigDecimal.ZERO) == 0;
    }

    public static SubscriptionView toSubscriptionView(Subscription s) {
        if (s == null) return null;
        long daysLeft = 0;
        if (s.getEndDate() != null && s.getEndDate().isAfter(LocalDateTime.now())) {
            long hours = Duration.between(LocalDateTime.now(), s.getEndDate()).toHours();
            daysLeft = (long) Math.ceil(hours / 24.0);
        }
        SubscriptionPlan p = s.getPlan();
        return new SubscriptionView(s.getId(), p.getId(), p.getName(), p.getPrice(),
                s.getStartDate(), s.getEndDate(), s.getStatus(), daysLeft, isTrial(p));
    }

    public static PaymentView toPaymentView(Payment p, String qrCodeUrl) {
        return new PaymentView(p.getId(), p.getSubscription().getId(), p.getSubscription().getPlan().getName(),
                p.getSubscription().getPlan().getDurationDays(),
                p.getAmount(), p.getMethod(), p.getStatus(), p.getPayUrl(), qrCodeUrl,
                p.getCreatedAt(), p.getPaidAt(), p.getTransferCode(), p.getCustomerPaidAt(),
                p.getStatus() == com.mael.entity.enums.PaymentStatus.FAILED ? p.getAdminNote() : null);
    }

    public static AgentView toAgentView(SessionAgent sa) {
        return new AgentView(sa.getId(), sa.getAgent().getId(), sa.getAgent().getName(), sa.getRoleInSession());
    }

    public static MessageView toMessageView(Message m) {
        SessionAgent sa = m.getSessionAgent();
        return new MessageView(
                m.getId(),
                m.getSenderType(),
                sa != null ? sa.getAgent().getId() : null,
                sa != null ? sa.getAgent().getName() : null,
                sa != null ? sa.getRoleInSession() : null,
                m.getContent(),
                m.getReplyTo() != null ? m.getReplyTo().getId() : null,
                m.getQuickReplies() == null ? List.of() : m.getQuickReplies().stream().map(QuickReply::text).toList(),
                m.getCreatedAt());
    }

    public static PhotoProblemView toPhotoView(PhotoProblem p) {
        Solution s = p.getSolution();
        SolutionView sv = s == null ? null : new SolutionView(
                s.getId(), s.getFinalAnswer(), splitSteps(s.getSteps()), s.getGeneratedBy(), s.getCreatedAt());
        return new PhotoProblemView(p.getId(), p.getImageUrl(), p.getExtractedText(), p.getStatus(),
                p.getTopic() != null ? p.getTopic().getId() : null,
                p.getTopic() != null ? p.getTopic().getName() : null,
                p.getUploadedAt(), sv);
    }

    public static WeaknessView toWeaknessView(Weakness w) {
        if (w == null) return null;
        return new WeaknessView(w.getId(), w.getTopic().getId(), w.getTopic().getName(),
                w.getMasteryScore(), w.getSeverity(), w.getStatus());
    }

    public static NotificationView toNotificationView(Notification n) {
        return new NotificationView(n.getId(), n.getType(), n.getTitle(), n.getContent(), n.getRead(), n.getCreatedAt());
    }

    private static List<String> splitSteps(String steps) {
        if (steps == null || steps.isBlank()) return List.of();
        return Arrays.stream(steps.split("\\R")).map(String::trim).filter(x -> !x.isEmpty()).toList();
    }
}

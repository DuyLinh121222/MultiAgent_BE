package com.mael.entity;

import com.mael.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Giao dịch thanh toán. orderId gửi MoMo = id, transId MoMo trả về = transactionCode.
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @UuidGenerator
    @Column(name = "payment_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String method = "MOMO_WALLET";

    @Column(name = "request_id", unique = true, length = 64)
    private String requestId;

    @Column(name = "transaction_code", unique = true, length = 64)
    private String transactionCode;

    @Column(name = "pay_url", length = 1000)
    private String payUrl;

    @Column(name = "result_code")
    private Integer resultCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ipn_payload", columnDefinition = "jsonb")
    private String ipnPayload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

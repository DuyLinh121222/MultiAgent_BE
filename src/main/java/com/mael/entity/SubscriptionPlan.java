package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan {

    @Id
    @UuidGenerator
    @Column(name = "plan_id", length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    /** null = không giới hạn số phiên thảo luận AI */
    @Column(name = "max_ai_sessions")
    private Integer maxAiSessions;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}

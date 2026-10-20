package com.mael.entity;

import com.mael.entity.enums.PlanAudience;
import com.mael.entity.enums.PriceUnit;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PlanAudience audience = PlanAudience.STUDENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", nullable = false, length = 20)
    @Builder.Default
    private PriceUnit priceUnit = PriceUnit.PACKAGE;

    /** Số học sinh tối thiểu (gói trung tâm / trường học) */
    @Column(name = "min_seats")
    private Integer minSeats;

    /** Quyền lợi, mỗi dòng một ý */
    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean featured = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}

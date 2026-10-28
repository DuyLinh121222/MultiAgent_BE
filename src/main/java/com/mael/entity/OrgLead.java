package com.mael.entity;

import com.mael.entity.enums.LeadStatus;
import com.mael.entity.enums.OrgType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Trung tâm / trường học để lại thông tin nhận tư vấn gói tổ chức.
 */
@Entity
@Table(name = "org_lead")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgLead {

    @Id
    @UuidGenerator
    @Column(name = "lead_id", length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "org_type", nullable = false, length = 10)
    private OrgType orgType;

    @Column(name = "org_name", nullable = false, length = 200)
    private String orgName;

    @Column(name = "contact_name", nullable = false, length = 150)
    private String contactName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "student_count")
    private Integer studentCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private SubscriptionPlan plan;

    @Column(columnDefinition = "TEXT")
    private String note;

    /** IP gửi yêu cầu (chống spam) */
    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    /** Ghi chú nội bộ của admin (đã gọi, hẹn lịch demo...) */
    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}

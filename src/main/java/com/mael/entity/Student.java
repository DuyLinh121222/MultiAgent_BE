package com.mael.entity;

import com.mael.entity.enums.LearningGoal;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @UuidGenerator
    @Column(name = "student_id", length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserAccount user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /** Đã qua màn chào mừng 3 bước chưa (học sinh mới đăng ký = false) */
    @Column(nullable = false)
    @Builder.Default
    private Boolean onboarded = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_goal", length = 20)
    private LearningGoal learningGoal;

    /** Xu nhận được khi qua ải, dùng đổi thú đồng hành */
    @Column(nullable = false)
    @Builder.Default
    private Integer coins = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer xp = 0;

    /** Thú đồng hành đang đi cùng trên bản đồ ải */
    @Column(name = "active_pet_code", length = 50)
    private String activePetCode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "achievement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Achievement {

    @Id
    @UuidGenerator
    @Column(name = "achievement_id", length = 36)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(columnDefinition = "TEXT")
    private String criteria;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 0;

    /** Nhóm hiển thị: LEARN | STREAK | GAME | COLLECT | SOCIAL */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String category = "LEARN";

    /** Chỉ số tự tính để mở huy hiệu (xem AchievementService.Metric). null = trao thủ công */
    @Column(length = 30)
    private String metric;

    /** Mốc cần đạt của metric */
    private Integer target;

    /** 1 đồng, 2 bạc, 3 vàng, 4 kim cương */
    @Column(nullable = false)
    @Builder.Default
    private Integer tier = 1;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}

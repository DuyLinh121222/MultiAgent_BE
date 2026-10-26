package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Bài viết / tin tức hiện ở banner trang chủ cho khách. Admin và giảng viên đăng.
 */
@Entity
@Table(name = "blog_post")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogPost {

    @Id
    @UuidGenerator
    @Column(name = "post_id", length = 36)
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "cover_url", length = 1000)
    private String coverUrl;

    /** Màu banner khi không có ảnh bìa: BLUE | ORANGE | GREEN | PURPLE | PINK */
    @Column(nullable = false, length = 10)
    @Builder.Default
    private String theme = "BLUE";

    @Column(length = 50)
    private String tag;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean published = true;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private Boolean pinned = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserAccount author;

    @Column(name = "author_name", length = 150)
    private String authorName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.mael.entity;

import com.mael.entity.enums.ReviewDecision;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Lịch sử duyệt nội dung. Mỗi dòng trỏ đúng 1 trong (lesson, question) - có CHECK trong DB.
 */
@Entity
@Table(name = "content_review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentReview {

    @Id
    @UuidGenerator
    @Column(name = "review_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private LocalDateTime reviewedAt;

    @PrePersist
    void onCreate() {
        if (reviewedAt == null) {
            reviewedAt = LocalDateTime.now();
        }
    }
}

package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Kết quả đi ải của học sinh: ải thường (lesson != null) hoặc ải trùm cuối chương (topic != null).
 */
@Entity
@Table(name = "stage_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageResult {

    @Id
    @UuidGenerator
    @Column(name = "stage_result_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private Topic topic;

    /** 0-3 sao, đạt từ 1 sao là qua ải */
    @Column(nullable = false)
    @Builder.Default
    private Integer stars = 0;

    @Column(name = "best_correct", nullable = false)
    @Builder.Default
    private Integer bestCorrect = 0;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private Integer totalQuestions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "first_cleared_at")
    private LocalDateTime firstClearedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }
}

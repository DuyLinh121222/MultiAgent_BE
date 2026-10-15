package com.mael.entity;

import com.mael.entity.enums.GeneratedBy;
import com.mael.entity.enums.QuizType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quiz {

    @Id
    @UuidGenerator
    @Column(name = "quiz_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weakness_id")
    private Weakness weakness;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", nullable = false, length = 20)
    @Builder.Default
    private QuizType quizType = QuizType.PRACTICE;

    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "generated_by", nullable = false, length = 10)
    @Builder.Default
    private GeneratedBy generatedBy = GeneratedBy.SYSTEM;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<QuizQuestion> questions = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

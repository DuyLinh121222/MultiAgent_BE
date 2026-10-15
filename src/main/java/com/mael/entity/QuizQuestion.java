package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "quiz_question",
        uniqueConstraints = @UniqueConstraint(name = "uq_quiz_question", columnNames = {"quiz_id", "question_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizQuestion {

    @Id
    @UuidGenerator
    @Column(name = "quiz_question_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal points = BigDecimal.ONE;
}

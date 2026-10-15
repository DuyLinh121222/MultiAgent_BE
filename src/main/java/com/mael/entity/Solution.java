package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "solution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solution {

    @Id
    @UuidGenerator
    @Column(name = "solution_id", length = 36)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_problem_id", nullable = false, unique = true)
    private PhotoProblem photoProblem;

    @Column(name = "final_answer", columnDefinition = "TEXT")
    private String finalAnswer;

    /** Các bước giải, mỗi bước 1 dòng */
    @Column(columnDefinition = "TEXT")
    private String steps;

    /** Tên model AI đã sinh lời giải */
    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

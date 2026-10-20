package com.mael.entity;

import com.mael.entity.enums.PetSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_pet",
        uniqueConstraints = @UniqueConstraint(name = "uq_student_pet", columnNames = {"student_id", "pet_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPet {

    @Id
    @UuidGenerator
    @Column(name = "student_pet_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_code", nullable = false)
    private Pet pet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetSource source;

    @Column(name = "obtained_at", nullable = false, updatable = false)
    private LocalDateTime obtainedAt;

    @PrePersist
    void onCreate() {
        if (obtainedAt == null) obtainedAt = LocalDateTime.now();
    }
}

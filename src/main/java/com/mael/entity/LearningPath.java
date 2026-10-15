package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "learning_path")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPath {

    @Id
    @UuidGenerator
    @Column(name = "path_id", length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @OneToMany(mappedBy = "learningPath")
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Topic> topics = new ArrayList<>();
}

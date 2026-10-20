package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "topic")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @UuidGenerator
    @Column(name = "topic_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "path_id", nullable = false)
    private LearningPath learningPath;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /** Học kỳ 1 hoặc 2 */
    @Column(nullable = false)
    @Builder.Default
    private Integer semester = 1;

    /** Thú trong rương báu khi phá đảo ải trùm cuối chương (FK tới pet.pet_code) */
    @Column(name = "reward_pet_code", length = 50)
    private String rewardPetCode;

    @OneToMany(mappedBy = "topic")
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<Lesson> lessons = new ArrayList<>();
}

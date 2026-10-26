package com.mael.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

/**
 * Một lượt chơi mini game Đấu trường của học sinh.
 */
@Entity
@Table(name = "arena_play")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArenaPlay {

    @Id
    @UuidGenerator
    @Column(name = "play_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false)
    @Builder.Default
    private Integer correct = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer total = 0;

    @Column(name = "best_combo", nullable = false)
    @Builder.Default
    private Integer bestCombo = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    @Column(name = "coins_earned", nullable = false)
    @Builder.Default
    private Integer coinsEarned = 0;

    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;

    @PrePersist
    void onCreate() {
        if (playedAt == null) playedAt = LocalDateTime.now();
    }
}

package com.mael.repository;

import com.mael.entity.ArenaPlay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ArenaPlayRepository extends JpaRepository<ArenaPlay, String> {

    long countByStudentId(String studentId);

    @Query("select coalesce(max(a.bestCombo), 0) from ArenaPlay a where a.student.id = :studentId")
    int maxCombo(String studentId);

    @Query("select coalesce(sum(a.coinsEarned), 0) from ArenaPlay a where a.student.id = :studentId and a.playedAt >= :from")
    int coinsSince(String studentId, LocalDateTime from);

    @Query("select coalesce(max(a.score), 0) from ArenaPlay a where a.student.id = :studentId and a.topic.id = :topicId")
    int bestScore(String studentId, String topicId);

    List<ArenaPlay> findTop10ByStudentIdOrderByPlayedAtDesc(String studentId);
}

package com.mael.repository;

import com.mael.entity.DiscussionSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface DiscussionSessionRepository extends JpaRepository<DiscussionSession, String> {

    @Query("select s from DiscussionSession s join fetch s.lesson where s.student.id = :studentId order by s.startedAt desc")
    List<DiscussionSession> findByStudentId(String studentId);

    long countByStudentIdAndStartedAtAfter(String studentId, LocalDateTime after);

    long countByStudentId(String studentId);
}

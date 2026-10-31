package com.mael.repository;

import com.mael.entity.DiscussionSession;
import com.mael.entity.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DiscussionSessionRepository extends JpaRepository<DiscussionSession, String> {

    @Query("select s from DiscussionSession s join fetch s.lesson where s.student.id = :studentId order by s.startedAt desc")
    List<DiscussionSession> findByStudentId(String studentId);

    long countByStudentIdAndStartedAtAfter(String studentId, LocalDateTime after);

    long countByStudentId(String studentId);

    /** Phiên đang mở gần nhất của học sinh cho bài này (để vào lại không mất đoạn chat cũ) */
    Optional<DiscussionSession> findFirstByStudentIdAndLessonIdAndStatusOrderByStartedAtDesc(
            String studentId, String lessonId, SessionStatus status);
}

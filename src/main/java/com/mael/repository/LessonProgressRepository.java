package com.mael.repository;

import com.mael.entity.LessonProgress;
import com.mael.entity.enums.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, String> {

    Optional<LessonProgress> findByStudentIdAndLessonId(String studentId, String lessonId);

    List<LessonProgress> findByStudentId(String studentId);

    long countByStudentIdAndStatus(String studentId, ProgressStatus status);

    @Query("select p from LessonProgress p join fetch p.lesson l join fetch l.topic where p.student.id = :studentId order by p.lastAccessedAt desc nulls last")
    List<LessonProgress> findRecent(String studentId);
}

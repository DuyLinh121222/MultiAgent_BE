package com.mael.repository;

import com.mael.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, String> {

    @Query("select a from QuizAttempt a join fetch a.quiz where a.student.id = :studentId order by a.startedAt desc")
    List<QuizAttempt> findByStudentId(String studentId);
}

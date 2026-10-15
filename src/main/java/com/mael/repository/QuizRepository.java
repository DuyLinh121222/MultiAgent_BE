package com.mael.repository;

import com.mael.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, String> {

    @Query("select distinct q from Quiz q left join fetch q.questions qq left join fetch qq.question where q.id = :id")
    Optional<Quiz> findWithQuestions(String id);

    /** Quiz chung (không gắn điểm yếu) + quiz gắn điểm yếu của học sinh này */
    @Query("select q from Quiz q left join q.weakness w where w is null or w.student.id = :studentId order by q.createdAt desc")
    List<Quiz> findAvailableForStudent(String studentId);
}

package com.mael.repository;

import com.mael.entity.StageResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageResultRepository extends JpaRepository<StageResult, String> {

    List<StageResult> findByStudentId(String studentId);

    Optional<StageResult> findByStudentIdAndLessonId(String studentId, String lessonId);

    Optional<StageResult> findByStudentIdAndTopicId(String studentId, String topicId);
}

package com.mael.repository;

import com.mael.entity.LearningPath;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearningPathRepository extends JpaRepository<LearningPath, String> {

    List<LearningPath> findByActiveTrueOrderByGradeLevelAsc();

    Optional<LearningPath> findFirstByGradeLevelAndActiveTrue(Integer gradeLevel);
}

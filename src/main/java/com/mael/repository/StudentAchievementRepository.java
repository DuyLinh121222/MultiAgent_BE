package com.mael.repository;

import com.mael.entity.StudentAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, String> {

    @Query("select sa from StudentAchievement sa join fetch sa.achievement where sa.student.id = :studentId order by sa.earnedAt desc")
    List<StudentAchievement> findByStudentId(String studentId);

    boolean existsByStudentIdAndAchievementCode(String studentId, String code);
}

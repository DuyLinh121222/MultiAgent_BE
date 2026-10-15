package com.mael.repository;

import com.mael.entity.Weakness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WeaknessRepository extends JpaRepository<Weakness, String> {

    @Query("select w from Weakness w join fetch w.topic where w.student.id = :studentId order by w.masteryScore asc")
    List<Weakness> findByStudentId(String studentId);

    Optional<Weakness> findByStudentIdAndTopicId(String studentId, String topicId);
}

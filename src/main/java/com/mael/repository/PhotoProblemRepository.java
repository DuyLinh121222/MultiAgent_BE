package com.mael.repository;

import com.mael.entity.PhotoProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PhotoProblemRepository extends JpaRepository<PhotoProblem, String> {

    @Query("select p from PhotoProblem p left join fetch p.solution left join fetch p.topic where p.student.id = :studentId order by p.uploadedAt desc")
    List<PhotoProblem> findByStudentId(String studentId);

    long countByStudentId(String studentId);
}

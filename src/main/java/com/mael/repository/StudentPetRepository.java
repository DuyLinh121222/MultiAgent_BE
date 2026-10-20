package com.mael.repository;

import com.mael.entity.StudentPet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentPetRepository extends JpaRepository<StudentPet, String> {

    List<StudentPet> findByStudentId(String studentId);

    boolean existsByStudentIdAndPetCode(String studentId, String petCode);
}

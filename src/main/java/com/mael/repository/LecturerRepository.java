package com.mael.repository;

import com.mael.entity.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, String> {

    Optional<Lecturer> findByUserId(String userId);
}

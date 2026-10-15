package com.mael.repository;

import com.mael.entity.LessonMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, String> {
}

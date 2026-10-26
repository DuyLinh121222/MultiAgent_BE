package com.mael.repository;

import com.mael.entity.LessonMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonMaterialRepository extends JpaRepository<LessonMaterial, String> {

    @org.springframework.data.jpa.repository.Query("select m from LessonMaterial m join fetch m.lesson l join fetch l.topic where m.uploadedBy.id = :lecturerId order by m.uploadedAt desc")
    java.util.List<LessonMaterial> findByLecturer(String lecturerId);

    java.util.List<LessonMaterial> findByLessonId(String lessonId);
}

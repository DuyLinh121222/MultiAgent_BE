package com.mael.repository;

import com.mael.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByStudentIdOrderByCreatedAtDesc(String studentId);

    long countByStudentIdAndReadFalse(String studentId);
}

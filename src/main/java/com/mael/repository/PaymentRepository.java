package com.mael.repository;

import com.mael.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {

    @Query("select p from Payment p join fetch p.subscription s join fetch s.plan join fetch s.student where p.id = :id")
    Optional<Payment> findDetailById(String id);

    boolean existsByTransferCode(String transferCode);

    @Query("select p from Payment p join fetch p.subscription s join fetch s.plan where s.student.id = :studentId order by p.createdAt desc")
    List<Payment> findByStudentId(String studentId);
}

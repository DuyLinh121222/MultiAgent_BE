package com.mael.repository;

import com.mael.entity.Subscription;
import com.mael.entity.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, String> {

    @Query("select s from Subscription s join fetch s.plan where s.student.id = :studentId order by s.createdAt desc")
    List<Subscription> findByStudentId(@Param("studentId") String studentId);

    /** Gói đang có hiệu lực của học sinh, lấy gói hết hạn muộn nhất */
    @Query("""
            select s from Subscription s join fetch s.plan
            where s.student.id = :studentId
              and s.status = com.mael.entity.enums.SubscriptionStatus.ACTIVE
              and s.endDate > :now
            order by s.endDate desc
            limit 1
            """)
    Optional<Subscription> findCurrentActive(@Param("studentId") String studentId, @Param("now") LocalDateTime now);

    /** Đã từng đăng ký gói có giá này chưa (dùng để chặn dùng thử 2 lần) */
    boolean existsByStudentIdAndPlanPrice(String studentId, BigDecimal price);

    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime time);
}

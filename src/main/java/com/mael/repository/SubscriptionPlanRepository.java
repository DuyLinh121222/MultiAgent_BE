package com.mael.repository;

import com.mael.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, String> {

    List<SubscriptionPlan> findByActiveTrueOrderByPriceAsc();

    /** Gói dùng thử = gói miễn phí đang hoạt động */
    Optional<SubscriptionPlan> findFirstByActiveTrueAndPrice(BigDecimal price);
}

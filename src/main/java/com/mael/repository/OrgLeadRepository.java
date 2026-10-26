package com.mael.repository;

import com.mael.entity.OrgLead;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgLeadRepository extends JpaRepository<OrgLead, String> {

    List<OrgLead> findAllByOrderByCreatedAtDesc();

    long countByPhoneAndCreatedAtAfter(String phone, java.time.LocalDateTime after);

    long countByIpAddressAndCreatedAtAfter(String ipAddress, java.time.LocalDateTime after);
}

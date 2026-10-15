package com.mael.repository;

import com.mael.entity.AiAgent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentRepository extends JpaRepository<AiAgent, String> {
}

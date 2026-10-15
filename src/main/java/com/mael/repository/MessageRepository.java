package com.mael.repository;

import com.mael.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, String> {

    @Query("""
            select m from Message m
            left join fetch m.sessionAgent sa left join fetch sa.agent
            where m.session.id = :sessionId order by m.createdAt asc, m.id asc
            """)
    List<Message> findBySessionId(String sessionId);

    long countBySessionId(String sessionId);
}

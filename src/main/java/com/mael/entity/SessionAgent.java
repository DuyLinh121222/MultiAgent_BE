package com.mael.entity;

import com.mael.entity.enums.AgentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_agent",
        uniqueConstraints = @UniqueConstraint(name = "uq_session_agent", columnNames = {"session_id", "agent_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionAgent {

    @Id
    @UuidGenerator
    @Column(name = "session_agent_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DiscussionSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_id", nullable = false)
    private AiAgent agent;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_session", nullable = false, length = 10)
    private AgentType roleInSession;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}

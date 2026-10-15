package com.mael.entity;

import com.mael.entity.enums.AgentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ai_agent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAgent {

    @Id
    @UuidGenerator
    @Column(name = "agent_id", length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_type", nullable = false, length = 10)
    private AgentType agentType;

    /** System prompt định nghĩa vai của tác tử */
    @Column(name = "persona_prompt", columnDefinition = "TEXT")
    private String personaPrompt;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}

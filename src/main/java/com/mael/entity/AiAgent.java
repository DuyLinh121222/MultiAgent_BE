package com.mael.entity;

import com.mael.entity.enums.AgentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

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

    /** Giọng Google Text-to-Speech, ví dụ vi-VN-Wavenet-D */
    @Column(name = "voice_name", length = 50)
    private String voiceName;

    /** Cao độ giọng (semitone, -20 đến 20) */
    @Column(name = "voice_pitch", nullable = false, precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal voicePitch = BigDecimal.ZERO;

    /** Tốc độ đọc (0.25 - 4.0) */
    @Column(name = "voice_rate", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal voiceRate = BigDecimal.ONE;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean active = true;
}

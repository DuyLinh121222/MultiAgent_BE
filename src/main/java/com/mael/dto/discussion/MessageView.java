package com.mael.dto.discussion;

import com.mael.entity.enums.AgentType;
import com.mael.entity.enums.SenderType;

import java.time.LocalDateTime;

/**
 * Không trả cờ hasIntendedError cho học sinh, để học sinh tự phát hiện lỗi sai (xem API flag-error).
 */
public record MessageView(
        String id,
        SenderType senderType,
        String agentId,
        String agentName,
        AgentType agentType,
        String content,
        String replyToId,
        LocalDateTime createdAt
) {
}

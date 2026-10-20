package com.mael.dto.discussion;

import com.mael.entity.enums.AgentType;
import com.mael.entity.enums.SenderType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Không trả cờ hasIntendedError / errorNote / đáp án đúng của nút trả lời nhanh cho học sinh,
 * để học sinh tự phát hiện lỗi sai (xem API flag-error).
 *
 * @param quickReplies các nút học sinh bấm để trả lời (chỉ hiện với tin nhắn AI cuối cùng)
 */
public record MessageView(
        String id,
        SenderType senderType,
        String agentId,
        String agentName,
        AgentType agentType,
        String content,
        String replyToId,
        List<String> quickReplies,
        LocalDateTime createdAt
) {
}

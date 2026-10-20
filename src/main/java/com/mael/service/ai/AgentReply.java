package com.mael.service.ai;

import com.mael.entity.QuickReply;

import java.util.List;

/**
 * Một câu trả lời của tác tử AI.
 *
 * @param agentId          id của AI_AGENT trả lời
 * @param content          nội dung
 * @param hasIntendedError true nếu đây là câu phản biện cố ý sai
 * @param errorNote        giải thích chỗ sai (chỉ hiện sau khi học sinh bắt được lỗi)
 * @param quickReplies     các nút để học sinh bấm chọn thay vì gõ (null = không có)
 */
public record AgentReply(String agentId, String content, boolean hasIntendedError, String errorNote,
                         List<QuickReply> quickReplies) {

    public static AgentReply say(String agentId, String content) {
        return new AgentReply(agentId, content, false, null, null);
    }

    public static AgentReply ask(String agentId, String content, List<QuickReply> quickReplies) {
        return new AgentReply(agentId, content, false, null, quickReplies);
    }

    public static AgentReply mistake(String agentId, String content, String errorNote) {
        return new AgentReply(agentId, content, true, errorNote, null);
    }
}

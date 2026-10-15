package com.mael.service.ai;

/**
 * Một câu trả lời của tác tử AI.
 *
 * @param agentId          id của AI_AGENT trả lời
 * @param content          nội dung
 * @param hasIntendedError true nếu đây là câu phản biện cố ý sai logic
 */
public record AgentReply(String agentId, String content, boolean hasIntendedError) {
}

package com.mael.service.ai;

import com.mael.entity.enums.AgentType;

import java.util.List;

/**
 * Ngữ cảnh gửi cho AI: bài giảng đã duyệt + danh sách tác tử + lịch sử hội thoại.
 * Khi tích hợp OpenAI, đặt lessonContent ở đầu prompt để tận dụng cached input.
 */
public record DiscussionContext(
        String sessionId,
        String studentName,
        String lessonTitle,
        String lessonContent,
        String topicName,
        List<AgentInfo> agents,
        List<Turn> history
) {
    public record AgentInfo(String agentId, String name, AgentType type, String personaPrompt, String modelName) {
    }

    /** senderName = tên AI hoặc tên học sinh */
    public record Turn(String senderName, boolean fromStudent, String content) {
    }

    public AgentInfo tutor() {
        return agents.stream().filter(a -> a.type() == AgentType.TUTOR).findFirst().orElse(null);
    }

    public List<AgentInfo> critics() {
        return agents.stream().filter(a -> a.type() == AgentType.CRITIC).toList();
    }

    public long studentTurns() {
        return history.stream().filter(Turn::fromStudent).count();
    }
}

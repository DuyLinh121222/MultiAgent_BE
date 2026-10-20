package com.mael.service.ai;

import com.mael.entity.Misconception;
import com.mael.entity.QuickReply;
import com.mael.entity.enums.AgentType;

import java.util.List;

/**
 * Ngữ cảnh gửi cho AI: bài giảng đã duyệt + lỗi sai hay gặp của bài + danh sách tác tử + lịch sử hội thoại.
 * Khi tích hợp OpenAI, đặt lessonContent + misconceptions ở đầu prompt để tận dụng cached input,
 * và yêu cầu bạn phản biện CHỈ cố ý sai trong phạm vi misconceptions (tránh lạc đề sang bài khác).
 *
 * @param pendingReplies các nút trả lời nhanh của tin nhắn AI gần nhất (học sinh đang trả lời câu này)
 * @param maxRound       lượt hỏi lớn nhất đã đưa ra trong phiên (-1 nếu chưa có)
 */
public record DiscussionContext(
        String sessionId,
        String studentName,
        String lessonTitle,
        String lessonContent,
        String topicName,
        List<Misconception> misconceptions,
        List<AgentInfo> agents,
        List<Turn> history,
        List<QuickReply> pendingReplies,
        int maxRound
) {
    public record AgentInfo(String agentId, String name, AgentType type, String personaPrompt, String modelName) {
    }

    /** senderName = tên AI hoặc tên học sinh */
    public record Turn(String senderName, boolean fromStudent, String content, boolean hasIntendedError) {
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

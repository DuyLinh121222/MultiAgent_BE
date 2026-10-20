package com.mael.service.ai;

import com.mael.entity.QuickReply;

import java.util.List;

/**
 * Điều phối các tác tử AI (gia sư + phản biện) trong phòng thảo luận.
 *
 * Hiện tại dùng {@link MockAiOrchestrator} (kịch bản dựng từ nội dung + lỗi sai hay gặp của bài, không gọi API).
 * Khi tích hợp OpenAI: tạo class mới implements interface này, đánh dấu @Service @Primary
 * hoặc đổi app.ai.provider=openai, gọi Responses API với persona_prompt của từng AI_AGENT.
 * Yêu cầu model trả JSON: content, has_intended_error, error_note, quick_replies[{text, correct, catch_error}].
 */
public interface AiOrchestrator {

    /** Tin nhắn mở đầu khi học sinh vào phòng */
    List<AgentReply> openingMessages(DiscussionContext context);

    /**
     * Các AI trả lời sau khi học sinh gửi tin nhắn.
     *
     * @param selected nút trả lời nhanh học sinh vừa bấm (null nếu học sinh tự gõ)
     */
    List<AgentReply> reply(DiscussionContext context, String studentMessage, QuickReply selected);

    /** Tóm tắt cuối phiên (lưu vào DISCUSSION_SESSION.summary) */
    String summarize(DiscussionContext context);
}

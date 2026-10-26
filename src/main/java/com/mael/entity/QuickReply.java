package com.mael.entity;

/**
 * Lựa chọn trả lời nhanh gắn với tin nhắn AI (lưu jsonb trong MESSAGE.quick_replies).
 *
 * @param text       nội dung nút học sinh bấm
 * @param correct    true = đáp án đúng, false = sai, null = không chấm (ví dụ "Em chưa chắc")
 * @param catchError true nếu nút này là học sinh chỉ ra đúng lỗi sai của bạn AI
 * @param round      lượt hỏi thứ mấy trong phiên (để AI biết học sinh đang trả lời câu nào)
 * @param hint       gợi ý khi học sinh chọn sai / chưa chắc (không lộ đáp án)
 * @param explain    lời chốt kiến thức khi học sinh chọn đúng
 * @param maker      agentId của bạn AI vừa phát biểu để học sinh phán xử (null nếu thầy hỏi)
 * @param kind       loại lượt: CATCH (bạn AI nói sai) | TRUE (bạn AI nói đúng) | APPLY (áp dụng công thức) | TF (thầy đố) | FREE
 */
public record QuickReply(String text, Boolean correct, Boolean catchError, Integer round,
                         String hint, String explain, String maker, String kind) {

    public static QuickReply neutral(String text, Integer round) {
        return new QuickReply(text, null, false, round, null, null, null, "FREE");
    }

    public QuickReply withText(String newText) {
        return new QuickReply(newText, correct, catchError, round, hint, explain, maker, kind);
    }
}

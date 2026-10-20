package com.mael.entity;

/**
 * Lựa chọn trả lời nhanh gắn với tin nhắn AI (lưu jsonb trong MESSAGE.quick_replies).
 *
 * @param text       nội dung nút học sinh bấm
 * @param correct    true = đáp án đúng, false = sai, null = không chấm (ví dụ "Em chưa chắc")
 * @param catchError true nếu nút này là học sinh chỉ ra lỗi sai của bạn AI
 * @param round      lượt hỏi thứ mấy trong phiên (để AI biết học sinh đang trả lời câu nào)
 */
public record QuickReply(String text, Boolean correct, Boolean catchError, Integer round) {

    public static QuickReply neutral(String text, Integer round) {
        return new QuickReply(text, null, false, round);
    }

    public static QuickReply right(String text, boolean catchError, int round) {
        return new QuickReply(text, true, catchError, round);
    }

    public static QuickReply wrong(String text, int round) {
        return new QuickReply(text, false, false, round);
    }
}

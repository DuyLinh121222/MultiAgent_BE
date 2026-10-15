package com.mael.service.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bản giả lập AI để chạy thử giao diện khi chưa có API key.
 * Thầy AI gợi mở, Tuấn Kiệt thỉnh thoảng cố ý nói sai, Bảo An soát lỗi / gợi ý cách khác.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiOrchestrator implements AiOrchestrator {

    /** Những lập luận sai phổ biến ở THCS, dùng cho bạn phản biện */
    private static final List<String> INTENDED_ERRORS = List.of(
            "Mình nghĩ giá trị tuyệt đối của số âm vẫn là số âm, ví dụ |-3| = -3 đúng không các bạn?",
            "Khi chuyển một số hạng sang vế kia mình giữ nguyên dấu cũng được mà, kết quả vẫn vậy thôi!",
            "(a + b)² = a² + b² thôi, đâu cần thêm 2ab đâu nhỉ?",
            "Hai góc kề bù thì luôn bằng nhau, mỗi góc 90° đúng không?",
            "√9 = ±3 vì cả 3 và -3 bình phương lên đều bằng 9 mà!",
            "Phương trình |x| = -5 có nghiệm x = -5 chứ, vì -5 là số âm mà."
    );

    @Override
    public List<AgentReply> openingMessages(DiscussionContext ctx) {
        List<AgentReply> replies = new ArrayList<>();
        var tutor = ctx.tutor();
        if (tutor != null) {
            replies.add(new AgentReply(tutor.agentId(),
                    "Chào " + ctx.studentName() + "! Hôm nay chúng ta cùng thảo luận bài \"" + ctx.lessonTitle()
                            + "\". " + firstSentence(ctx.lessonContent())
                            + " Em thử nói xem em hiểu ý chính của bài này là gì?", false));
        }
        var errorMaker = errorMaker(ctx);
        var peer = peer(ctx);
        if (errorMaker != null) {
            replies.add(new AgentReply(errorMaker.agentId(), pickError(ctx), true));
        }
        if (peer != null) {
            replies.add(new AgentReply(peer.agentId(),
                    "Khoan đã, mình thấy câu của Kiệt có gì đó sai sai... " + ctx.studentName()
                            + " thử kiểm tra giúp mình xem bạn ấy sai ở đâu nhé!", false));
        }
        return replies;
    }

    @Override
    public List<AgentReply> reply(DiscussionContext ctx, String studentMessage) {
        List<AgentReply> replies = new ArrayList<>();
        long turn = ctx.studentTurns();
        String msg = studentMessage.toLowerCase(Locale.ROOT);
        var tutor = ctx.tutor();
        var errorMaker = errorMaker(ctx);
        var peer = peer(ctx);

        if (tutor != null) {
            String text;
            if (msg.contains("không hiểu") || msg.contains("chưa hiểu") || msg.contains("giải thích")) {
                text = "Không sao, thầy giải thích lại nhé. " + firstSentence(ctx.lessonContent())
                        + " Em đọc lại ví dụ trong bài rồi thử làm từng bước, sai đâu thầy chỉ chỗ đó.";
            } else if (msg.contains("sai")) {
                text = "Tốt lắm! Em đã nhận ra có điểm chưa đúng. Em hãy giải thích vì sao sai để cả nhóm cùng hiểu nhé.";
            } else {
                text = "Cảm ơn em đã chia sẻ! Ý của em có phần đúng. Em thử áp dụng vào một ví dụ cụ thể trong bài \""
                        + ctx.lessonTitle() + "\" xem kết quả ra sao?";
            }
            replies.add(new AgentReply(tutor.agentId(), text, false));
        }

        // Cứ 2 lượt thì bạn phản biện cố ý nói sai một lần, lượt còn lại bạn sáng tạo gợi ý cách khác
        if (turn % 2 == 0 && errorMaker != null) {
            replies.add(new AgentReply(errorMaker.agentId(), pickError(ctx), true));
        } else if (peer != null || errorMaker != null) {
            var speaker = peer != null ? peer : errorMaker;
            replies.add(new AgentReply(speaker.agentId(),
                    "Mình có cách khác nè: thử vẽ trục số hoặc thay một giá trị cụ thể vào để kiểm tra lại đáp án. "
                            + "Làm vậy dễ phát hiện chỗ sai hơn đó!", false));
        }
        return replies;
    }

    @Override
    public String summarize(DiscussionContext ctx) {
        return "Phiên thảo luận bài \"" + ctx.lessonTitle() + "\" có " + ctx.studentTurns()
                + " lượt phát biểu của " + ctx.studentName()
                + ". Ghi nhớ: đọc kỹ định nghĩa, xét đủ các trường hợp và luôn kiểm tra lại kết quả.";
    }

    /** Bạn phản biện hay cố ý nói sai: CRITIC có persona chứa "cố ý" (mặc định CRITIC đầu tiên) */
    private static DiscussionContext.AgentInfo errorMaker(DiscussionContext ctx) {
        var critics = ctx.critics();
        return critics.stream()
                .filter(a -> a.personaPrompt() != null && a.personaPrompt().toLowerCase(Locale.ROOT).contains("cố ý"))
                .findFirst()
                .orElse(critics.isEmpty() ? null : critics.get(0));
    }

    /** Bạn học còn lại (sáng tạo / soát lỗi) */
    private static DiscussionContext.AgentInfo peer(DiscussionContext ctx) {
        var maker = errorMaker(ctx);
        return ctx.critics().stream().filter(a -> a != maker).findFirst().orElse(null);
    }

    private String pickError(DiscussionContext ctx) {
        int idx = Math.floorMod((ctx.lessonTitle() + ctx.studentTurns()).hashCode(), INTENDED_ERRORS.size());
        return INTENDED_ERRORS.get(idx);
    }

    private static String firstSentence(String content) {
        if (content == null || content.isBlank()) return "";
        String first = content.split("\\R")[0].trim();
        return first.endsWith(".") ? first : first + ".";
    }
}

package com.mael.service.ai;

import com.mael.entity.Misconception;
import com.mael.entity.QuickReply;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bản giả lập AI để chạy thử khi chưa có API key. Kịch bản dựng từ chính bài đang học:
 *
 * - Mỗi lượt dùng một lỗi sai khác nhau trong LESSON.misconceptions của bài.
 * - Lượt chẵn (0, 2, 4...): Tuấn Kiệt cố ý nói sai lỗi đó,
 *   Bảo An nhờ học sinh kiểm tra, học sinh bấm chọn "Kiệt sai rồi: ..." / "Kiệt nói đúng mà" / "Em chưa chắc".
 * - Lượt lẻ (1, 3, 5...): Thầy đố "Câu sau đúng hay sai?".
 * - Chọn đúng: thầy khen + chốt kiến thức, chuyển lượt mới. Chọn sai / chưa chắc: thầy gợi ý, hỏi lại.
 *
 * Bài chưa có misconceptions thì Kiệt KHÔNG nói sai (tránh lạc đề), chỉ hỏi chung về bài.
 */
@Service
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAiOrchestrator implements AiOrchestrator {

    private static final String UNSURE = "Em chưa chắc, gợi ý giúp em";

    @Override
    public List<AgentReply> openingMessages(DiscussionContext ctx) {
        List<AgentReply> replies = new ArrayList<>();
        var tutor = ctx.tutor();
        if (tutor != null) {
            replies.add(AgentReply.say(tutor.agentId(),
                    "Chào " + ctx.studentName() + "! Hôm nay chúng ta cùng thảo luận bài \"" + ctx.lessonTitle() + "\". "
                            + firstSentence(ctx.lessonContent())
                            + (hasMisconceptions(ctx) ? " Các bạn nói thử xem mình hiểu bài này thế nào nhé!" : "")));
        }
        if (hasMisconceptions(ctx)) {
            replies.addAll(startRound(ctx, 0));
        } else if (tutor != null) {
            replies.add(AgentReply.ask(tutor.agentId(), "Em đọc phần lý thuyết bên trái rồi chọn điều em muốn làm tiếp nhé.",
                    genericReplies(0)));
        }
        return replies;
    }

    @Override
    public List<AgentReply> reply(DiscussionContext ctx, String studentMessage, QuickReply selected) {
        var tutor = ctx.tutor();
        var maker = errorMaker(ctx);
        List<AgentReply> replies = new ArrayList<>();
        if (tutor == null) return replies;

        if (!hasMisconceptions(ctx)) {
            return genericReply(ctx, studentMessage);
        }

        int round = selected != null && selected.round() != null ? selected.round() : Math.max(ctx.maxRound(), 0);
        Misconception m = misconception(ctx, round);
        boolean catchRound = round % 2 == 0;

        // 1) Học sinh bấm đáp án đúng
        if (selected != null && Boolean.TRUE.equals(selected.correct())) {
            replies.add(AgentReply.say(tutor.agentId(),
                    (catchRound ? "Chính xác, em bắt lỗi rất tốt! " : "Đúng rồi, giỏi lắm! ") + m.fix()));
            if (catchRound && maker != null) {
                replies.add(AgentReply.say(maker.agentId(),
                        "À đúng thật, mình nhầm rồi. Cảm ơn " + ctx.studentName() + " nhé, lần sau mình sẽ nhớ!"));
            }
            int next = ctx.maxRound() + 1;
            if (next == ctx.misconceptions().size()) {
                // Đã đi hết các bẫy của bài: mời học sinh vào thử thách ải
                replies.add(AgentReply.ask(tutor.agentId(),
                        "Em đã vượt qua hết các bẫy của bài này rồi! Bấm \"Vào thử thách ải\" để nhận sao và xu nhé. "
                                + "Hoặc mình ôn lại một lượt nữa?",
                        List.of(QuickReply.neutral("Ôn lại một lượt nữa", next),
                                QuickReply.neutral("Thầy tóm tắt bài giúp em", next))));
            } else {
                replies.addAll(startRound(ctx, next));
            }
            return replies;
        }

        // 2) Học sinh bấm đáp án sai
        if (selected != null && Boolean.FALSE.equals(selected.correct())) {
            replies.add(AgentReply.ask(tutor.agentId(),
                    "Chưa đúng rồi em. Bạn hay nhầm ở chỗ: " + lowerFirst(m.why()) + " Em đọc lại phần lý thuyết rồi chọn lại nhé.",
                    pendingOrRound(ctx, round)));
            return replies;
        }

        String msg = studentMessage.toLowerCase(Locale.ROOT);

        // 3) "Ôn lại một lượt nữa" / "tóm tắt"
        if (msg.contains("ôn lại")) {
            replies.addAll(startRound(ctx, ctx.maxRound() + 1));
            return replies;
        }
        if (msg.contains("tóm tắt")) {
            replies.add(AgentReply.say(tutor.agentId(), "Tóm tắt bài \"" + ctx.lessonTitle() + "\": " + keyPoints(ctx)));
            replies.add(AgentReply.ask(tutor.agentId(), "Em sẵn sàng vào thử thách ải chưa?",
                    List.of(QuickReply.neutral("Ôn lại một lượt nữa", ctx.maxRound() + 1))));
            return replies;
        }

        // 4) Chưa chắc / chưa hiểu: gợi ý rồi hỏi lại đúng câu đang dở
        if (selected != null || msg.contains("chưa chắc") || msg.contains("không hiểu") || msg.contains("chưa hiểu")
                || msg.contains("giải thích") || msg.contains("gợi ý")) {
            replies.add(AgentReply.ask(tutor.agentId(),
                    "Không sao, thầy gợi ý nhé: chỗ này hay sai vì " + lowerFirst(m.why()) + " Nhớ lại lý thuyết: " + firstSentence(ctx.lessonContent())
                            + " Giờ em chọn lại xem nào.",
                    pendingOrRound(ctx, round)));
            return replies;
        }

        // 5) Học sinh tự gõ: khen, rồi kéo về đúng câu đang hỏi
        String lead = msg.contains("sai")
                ? "Em thấy có chỗ sai đúng không? Vậy sai ở đâu, em chọn lý do đúng nhất nhé."
                : "Cảm ơn em đã chia sẻ! Cùng quay lại câu đang hỏi: em chọn đáp án em thấy đúng nhé.";
        replies.add(AgentReply.ask(tutor.agentId(), lead, pendingOrRound(ctx, round)));
        return replies;
    }

    @Override
    public String summarize(DiscussionContext ctx) {
        StringBuilder sb = new StringBuilder("Phiên thảo luận bài \"" + ctx.lessonTitle() + "\" có " + ctx.studentTurns()
                + " lượt trả lời của " + ctx.studentName() + ".");
        if (hasMisconceptions(ctx)) {
            sb.append(" Ghi nhớ: ").append(keyPoints(ctx));
        } else {
            sb.append(" Ghi nhớ: đọc kỹ định nghĩa, xét đủ các trường hợp và luôn kiểm tra lại kết quả.");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------

    /** Mở một lượt hỏi mới: lượt chẵn Kiệt cố ý sai, lượt lẻ thầy đố đúng/sai */
    private List<AgentReply> startRound(DiscussionContext ctx, int round) {
        List<AgentReply> replies = new ArrayList<>();
        Misconception m = misconception(ctx, round);
        var tutor = ctx.tutor();
        var maker = errorMaker(ctx);
        var peer = peer(ctx);

        if (round % 2 == 0 && maker != null) {
            replies.add(AgentReply.mistake(maker.agentId(), m.wrong(), m.why() + " " + m.fix()));
            String prompt = "Khoan đã, mình thấy câu của " + firstName(maker.name()) + " có gì đó sai sai... "
                    + ctx.studentName() + " kiểm tra giúp mình xem bạn ấy đúng hay sai nhé!";
            String agentId = peer != null ? peer.agentId() : tutor.agentId();
            replies.add(AgentReply.ask(agentId, prompt, catchOptions(ctx, round, maker.name())));
        } else if (tutor != null) {
            replies.add(AgentReply.ask(tutor.agentId(),
                    "Thầy đố em: câu sau ĐÚNG hay SAI? \"" + stripQuestion(m.wrong()) + "\"",
                    checkOptions(ctx, round)));
        }
        return replies;
    }

    private List<QuickReply> catchOptions(DiscussionContext ctx, int round, String makerName) {
        Misconception m = misconception(ctx, round);
        String who = firstName(makerName);
        List<QuickReply> options = new ArrayList<>();
        options.add(QuickReply.right(who + " sai, vì " + lowerFirst(firstSentence(m.fix())), true, round));
        options.add(QuickReply.wrong(who + " nói đúng mà", round));
        Misconception other = otherMisconception(ctx, round);
        if (other != null) {
            options.add(QuickReply.wrong(who + " sai, vì " + lowerFirst(stripQuestion(other.wrong()).replaceAll("[.!?]+$", "")) + ".", round));
        }
        rotate(options, round);
        options.add(QuickReply.neutral(UNSURE, round));
        return options;
    }

    private List<QuickReply> checkOptions(DiscussionContext ctx, int round) {
        Misconception m = misconception(ctx, round);
        List<QuickReply> options = new ArrayList<>();
        options.add(QuickReply.right("Sai, vì " + lowerFirst(firstSentence(m.fix())), false, round));
        options.add(QuickReply.wrong("Đúng", round));
        rotate(options, round + 1);
        options.add(QuickReply.neutral(UNSURE, round));
        return options;
    }

    private List<QuickReply> pendingOrRound(DiscussionContext ctx, int round) {
        if (ctx.pendingReplies() != null && !ctx.pendingReplies().isEmpty()) return ctx.pendingReplies();
        return round % 2 == 0 && errorMaker(ctx) != null
                ? catchOptions(ctx, round, errorMaker(ctx).name())
                : checkOptions(ctx, round);
    }

    private List<AgentReply> genericReply(DiscussionContext ctx, String studentMessage) {
        var tutor = ctx.tutor();
        var peer = peer(ctx);
        String msg = studentMessage.toLowerCase(Locale.ROOT);
        List<AgentReply> replies = new ArrayList<>();
        int round = ctx.maxRound() + 1;
        if (msg.contains("không hiểu") || msg.contains("chưa hiểu") || msg.contains("giải thích")) {
            replies.add(AgentReply.say(tutor.agentId(), "Không sao, thầy giải thích lại nhé. " + keyPoints(ctx)));
        } else if (msg.contains("ví dụ")) {
            replies.add(AgentReply.say(tutor.agentId(), "Em xem ví dụ trong bài: " + exampleLine(ctx.lessonContent())));
        } else if (msg.contains("cách") && peer != null) {
            replies.add(AgentReply.say(peer.agentId(),
                    "Mình hay thay một giá trị cụ thể vào để kiểm tra lại đáp án, làm vậy dễ phát hiện chỗ sai lắm!"));
        } else {
            replies.add(AgentReply.say(tutor.agentId(), "Tốt lắm! Em đã sẵn sàng thì bấm \"Vào thử thách ải\" để nhận sao nhé."));
        }
        replies.add(AgentReply.ask(tutor.agentId(), "Em muốn làm gì tiếp?", genericReplies(round)));
        return replies;
    }

    private static List<QuickReply> genericReplies(int round) {
        return List.of(
                QuickReply.neutral("Em chưa hiểu, thầy giải thích lại giúp em", round),
                QuickReply.neutral("Thầy cho em một ví dụ", round),
                QuickReply.neutral("Có cách giải nào khác không?", round));
    }

    private static boolean hasMisconceptions(DiscussionContext ctx) {
        return ctx.misconceptions() != null && !ctx.misconceptions().isEmpty();
    }

    /** Mỗi lượt dùng một bẫy khác nhau của bài: lượt 0 dùng bẫy 0, lượt 1 dùng bẫy 1... rồi quay vòng */
    private static Misconception misconception(DiscussionContext ctx, int round) {
        List<Misconception> list = ctx.misconceptions();
        return list.get(Math.floorMod(round, list.size()));
    }

    private static Misconception otherMisconception(DiscussionContext ctx, int round) {
        List<Misconception> list = ctx.misconceptions();
        if (list.size() < 2) return null;
        Misconception current = misconception(ctx, round);
        int i = list.indexOf(current);
        return list.get((i + 1) % list.size());
    }

    private static String keyPoints(DiscussionContext ctx) {
        if (!hasMisconceptions(ctx)) return firstSentence(ctx.lessonContent());
        StringBuilder sb = new StringBuilder();
        for (Misconception m : ctx.misconceptions()) {
            sb.append("• ").append(firstSentence(m.fix())).append(' ');
        }
        return sb.toString().trim();
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

    /** Đảo vị trí đáp án đúng theo lượt để học sinh không bấm theo thói quen */
    private static void rotate(List<QuickReply> options, int round) {
        if (options.size() < 2) return;
        java.util.Collections.rotate(options, Math.floorMod(round * 7 + 1, options.size()));
    }

    private static String firstName(String fullName) {
        if (fullName == null) return "bạn ấy";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private static String stripQuestion(String s) {
        return s.replaceAll("\\s*(đúng không các bạn\\?|đúng không\\?|nhỉ\\?|mà!|thôi!)\\s*$", "").replaceAll("[!]+$", "").trim();
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String exampleLine(String content) {
        if (content == null) return "";
        for (String line : content.split("\\R")) {
            if (line.trim().startsWith("Ví dụ")) return line.trim();
        }
        return firstSentence(content);
    }

    static String firstSentence(String content) {
        if (content == null || content.isBlank()) return "";
        // Lấy dòng có nội dung đầu tiên, bỏ qua dòng tiêu đề kiểu "Các bước giải:" hoặc gạch đầu dòng
        String first = java.util.Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(l -> l.length() > 15 && !l.endsWith(":") && !l.startsWith("•"))
                .findFirst()
                .orElse(content.split("\\R")[0].trim().replaceAll(":$", ""));
        // Cắt ở dấu chấm kết thúc câu đầu tiên (bỏ qua số thập phân như 0,5 và dấu chấm trong "Chương I.")
        int cut = first.indexOf(". ");
        if (cut > 20) first = first.substring(0, cut + 1);
        return first.endsWith(".") || first.endsWith("!") || first.endsWith("?") ? first : first + ".";
    }
}

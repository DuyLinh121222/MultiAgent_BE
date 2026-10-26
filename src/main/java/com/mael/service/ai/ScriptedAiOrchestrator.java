package com.mael.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.mael.entity.Misconception;
import com.mael.entity.QuickReply;
import com.mael.service.ai.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Điều phối 3 AI trong phòng thảo luận.
 *
 * Phần CHẤM ĐÚNG / SAI luôn dựng từ dữ liệu đã được giảng viên duyệt (lỗi sai hay gặp của bài + câu luyện tập của bài),
 * nên đáp án đúng luôn chính xác. Mỗi lượt bốc ngẫu nhiên 1 trong 5 kiểu:
 *
 * <pre>
 *  CATCH     Tuấn Kiệt cố ý nói SAI, học sinh bắt lỗi
 *  TRUE      Tuấn Kiệt nói ĐÚNG (để học sinh không mặc định "Kiệt luôn sai")
 *  AN_WRONG  Bảo An nhầm, Kiệt nghi ngờ, học sinh phán xử
 *  APPLY     Bảo An nêu công thức / mẹo, rồi đố học sinh áp dụng vào một bài luyện tập của bài
 *  TF        Thầy đố nhanh: câu này đúng hay sai (câu có thể đúng hoặc sai)
 * </pre>
 *
 * Thứ tự ai nói trước cũng đổi ngẫu nhiên. Các lựa chọn có độ dài tương đương nhau và được xáo trộn,
 * học sinh không thể đoán "đáp án dài nhất là đáp án đúng".
 *
 * Khi bật Gemini (AI_PROVIDER=gemini + GEMINI_KEY_DISCUSSION): học sinh tự gõ câu hỏi thì 3 AI trả lời tự do
 * trong phạm vi bài, tóm tắt cuối buổi cũng do Gemini viết. Gemini lỗi / hết lượt thì dùng kịch bản dự phòng.
 */
@Service
@RequiredArgsConstructor
public class ScriptedAiOrchestrator implements AiOrchestrator {

    private static final String UNSURE = "Em chưa chắc, gợi ý giúp em";
    private static final String REVIEW_AGAIN = "Ôn thêm một lượt nữa";
    private static final String SUMMARY = "Thầy tóm tắt bài giúp em";
    private static final int MAX_REASON = 130;

    private enum Kind { CATCH, TRUE, AN_WRONG, APPLY, TF }

    private final GeminiClient gemini;

    // ==================================================================
    //  API
    // ==================================================================

    @Override
    public List<AgentReply> openingMessages(DiscussionContext ctx) {
        List<AgentReply> replies = new ArrayList<>();
        var tutor = ctx.tutor();
        if (tutor == null) return replies;
        Random rnd = rnd(ctx, -1, 1);
        String hello = pick(rnd,
                "Chào " + ctx.studentName() + "! Hôm nay cả nhóm cùng bàn bài \"" + ctx.lessonTitle() + "\" nhé.",
                "Chào " + ctx.studentName() + ", vào học thôi! Bài hôm nay: \"" + ctx.lessonTitle() + "\".",
                ctx.studentName() + " đến rồi! Mình cùng khám phá bài \"" + ctx.lessonTitle() + "\" nào.");
        replies.add(AgentReply.say(tutor.agentId(), hello + " " + firstSentence(ctx.lessonContent())));
        if (roundsTotal(ctx) > 0) {
            replies.addAll(buildRound(ctx, 0).messages);
        } else {
            replies.add(AgentReply.ask(tutor.agentId(), "Em đọc phần lý thuyết bên trái rồi chọn điều em muốn làm tiếp nhé.",
                    genericReplies(0)));
        }
        return replies;
    }

    @Override
    public List<AgentReply> reply(DiscussionContext ctx, String studentMessage, QuickReply selected) {
        var tutor = ctx.tutor();
        List<AgentReply> replies = new ArrayList<>();
        if (tutor == null) return replies;
        String msg = studentMessage.toLowerCase(Locale.ROOT).trim();

        // Nút "Ôn thêm" / "Tóm tắt" ở cuối buổi
        if (msg.equals(REVIEW_AGAIN.toLowerCase(Locale.ROOT)) || msg.contains("ôn lại") || msg.contains("ôn thêm")) {
            if (roundsTotal(ctx) == 0) return genericReply(ctx, studentMessage);
            replies.addAll(buildRound(ctx, ctx.maxRound() + 1).messages);
            return replies;
        }
        if (msg.contains("tóm tắt")) {
            replies.add(AgentReply.say(tutor.agentId(), "Tóm tắt bài \"" + ctx.lessonTitle() + "\": " + keyPoints(ctx)));
            replies.add(AgentReply.ask(tutor.agentId(), "Em sẵn sàng vào thử thách ải chưa? Hay mình ôn thêm một lượt?",
                    List.of(QuickReply.neutral(REVIEW_AGAIN, ctx.maxRound() + 1))));
            return replies;
        }

        if (roundsTotal(ctx) == 0) {
            return aiFreeReply(ctx, studentMessage).orElseGet(() -> genericReply(ctx, studentMessage));
        }

        int round = selected != null && selected.round() != null ? selected.round() : Math.max(ctx.maxRound(), 0);
        Random rnd = rnd(ctx, round, 7 + ctx.history().size());

        // 1) Chọn đúng
        if (selected != null && Boolean.TRUE.equals(selected.correct())) {
            String praise = pick(rnd, "Chính xác!", "Giỏi lắm!", "Đúng rồi, em tinh mắt thật!", "Tuyệt vời!", "Quá chuẩn luôn!");
            replies.add(AgentReply.say(tutor.agentId(), praise + " " + nz(selected.explain())));
            var maker = agentById(ctx, selected.maker());
            if (maker != null) {
                if ("CATCH".equals(selected.kind())) {
                    replies.add(AgentReply.say(maker.agentId(), pick(rnd,
                            "À đúng thật, mình nhầm rồi. Cảm ơn " + ctx.studentName() + " nha!",
                            "Ôi, bị " + ctx.studentName() + " bắt bài rồi. Lần sau mình nhớ!",
                            "Chuẩn rồi, mình sai chỗ đó. " + ctx.studentName() + " giỏi ghê!")));
                } else if ("TRUE".equals(selected.kind())) {
                    replies.add(AgentReply.say(maker.agentId(), pick(rnd,
                            "Hehe, lần này mình nói đúng đó nha! Không phải lúc nào mình cũng sai đâu.",
                            "Thấy chưa, mình cũng có lúc đúng mà! Cảm ơn " + ctx.studentName() + " đã tin mình.",
                            "Yeah! " + ctx.studentName() + " phán xử chuẩn quá.")));
                }
            }
            if ("APPLY".equals(selected.kind()) && peer(ctx) != null && rnd.nextBoolean()) {
                replies.add(AgentReply.say(peer(ctx).agentId(), pick(rnd,
                        "Thấy chưa, nắm công thức là giải nhanh ghê!",
                        "Đúng kiểu mình hay làm luôn. " + ctx.studentName() + " nhanh tay thật!")));
            }
            int next = ctx.maxRound() + 1;
            if (next >= roundsTotal(ctx)) {
                replies.add(AgentReply.ask(tutor.agentId(),
                        "Em đã vượt qua hết các thử thách thảo luận của bài này! Bấm \"Vào thử thách ải\" để nhận sao và xu nhé.",
                        List.of(QuickReply.neutral(REVIEW_AGAIN, next), QuickReply.neutral(SUMMARY, next))));
            } else {
                replies.addAll(buildRound(ctx, next).messages);
            }
            return replies;
        }

        // 2) Chọn sai hoặc "chưa chắc": một người gợi ý (thầy hoặc An), hỏi lại với thứ tự đáp án mới
        if (selected != null) {
            boolean wrong = Boolean.FALSE.equals(selected.correct());
            String hint = hintOf(ctx, round);
            var maker = agentById(ctx, hintMaker(ctx));
            if (wrong && "TRUE".equals(kindOf(ctx)) && maker != null) {
                replies.add(AgentReply.say(maker.agentId(), "Ơ, mình nói đúng thật mà! " + ctx.studentName() + " xem lại lý thuyết nha."));
            }
            var helper = peer(ctx) != null && rnd.nextInt(3) == 0 ? peer(ctx) : tutor;
            String lead = wrong
                    ? pick(rnd, "Chưa đúng rồi.", "Gần đúng rồi, nhưng chưa phải.", "Hmm, chưa chuẩn lắm.")
                    : pick(rnd, "Không sao, gợi ý nè:", "Để mình gợi ý nhé:");
            replies.add(AgentReply.ask(helper.agentId(), lead + " " + hint + " Em chọn lại nhé.",
                    reshuffle(ctx.pendingReplies().isEmpty() ? buildRound(ctx, round).options : ctx.pendingReplies(),
                            rnd(ctx, round, ctx.history().size()))));
            return replies;
        }

        // 3) Học sinh tự gõ: Gemini trả lời tự do (nếu bật), rồi kéo về câu đang hỏi
        List<QuickReply> pending = ctx.pendingReplies().isEmpty() ? buildRound(ctx, round).options : ctx.pendingReplies();
        Optional<List<AgentReply>> ai = aiFreeReply(ctx, studentMessage);
        if (ai.isPresent()) {
            replies.addAll(ai.get());
            replies.add(AgentReply.ask(tutor.agentId(), "Giờ mình quay lại câu đang dở nhé, em chọn đáp án em thấy đúng:", pending));
            return replies;
        }
        if (msg.contains("chưa chắc") || msg.contains("không hiểu") || msg.contains("chưa hiểu")
                || msg.contains("giải thích") || msg.contains("gợi ý")) {
            replies.add(AgentReply.ask(tutor.agentId(), "Thầy gợi ý nhé: " + hintOf(ctx, round) + " Giờ em chọn lại xem nào.", pending));
            return replies;
        }
        String lead = msg.contains("sai")
                ? "Em thấy có chỗ sai đúng không? Vậy sai ở đâu, em chọn lý do đúng nhất nhé."
                : "Cảm ơn em đã chia sẻ! Cùng quay lại câu đang hỏi: em chọn đáp án em thấy đúng nhé.";
        replies.add(AgentReply.ask(tutor.agentId(), lead, pending));
        return replies;
    }

    @Override
    public String summarize(DiscussionContext ctx) {
        if (gemini.enabled(GeminiClient.Task.DISCUSSION) && ctx.studentTurns() > 0) {
            String prompt = "Viết tóm tắt buổi thảo luận Toán cho học sinh lớp THCS, tối đa 3 câu, giọng thân thiện, "
                    + "nêu 1 điều em làm tốt và 1 điều cần nhớ. Chỉ dựa vào nội dung dưới đây.\n\nBÀI: " + ctx.lessonTitle()
                    + "\nGHI NHỚ: " + keyPoints(ctx) + "\n\nHỘI THOẠI:\n" + transcript(ctx, 16);
            Optional<String> text = gemini.generateText(GeminiClient.Task.DISCUSSION, null,
                    List.of(GeminiClient.Part.text(prompt)), 0.4, 300);
            if (text.isPresent()) return text.get();
        }
        StringBuilder sb = new StringBuilder("Phiên thảo luận bài \"" + ctx.lessonTitle() + "\" có " + ctx.studentTurns()
                + " lượt trả lời của " + ctx.studentName() + ".");
        sb.append(" Ghi nhớ: ").append(keyPoints(ctx));
        return sb.toString();
    }

    // ==================================================================
    //  Dựng một lượt
    // ==================================================================

    private record Round(List<AgentReply> messages, List<QuickReply> options) {
    }

    private Round buildRound(DiscussionContext ctx, int round) {
        Kind kind = scenario(ctx, round);
        var tutor = ctx.tutor();
        var kiet = errorMaker(ctx);
        var an = peer(ctx);
        Random rnd = rnd(ctx, round, 3);
        String student = ctx.studentName();
        List<AgentReply> msgs = new ArrayList<>();

        if (kind == Kind.APPLY) {
            DiscussionContext.PracticeQuestion q = practice(ctx, round);
            String tip = tip(ctx, round);
            var tipper = an != null ? an : tutor;
            msgs.add(AgentReply.say(tipper.agentId(), pick(rnd,
                    "Mẹo của mình nè: " + tip,
                    "Mình hay nhớ thế này: " + tip,
                    "Công thức bỏ túi: " + tip)));
            boolean kietAsks = kiet != null && rnd.nextInt(5) < 2;
            String ask = kietAsks
                    ? "Để xem ai nhanh hơn nhé! " + q.content()
                    : pick(rnd, "Giờ áp dụng ngay nhé " + student + ": " + q.content(),
                    "Thử sức nào " + student + "! " + q.content());
            List<QuickReply> options = new ArrayList<>();
            for (int i = 0; i < q.options().size(); i++) {
                boolean ok = i == q.correctIndex();
                options.add(new QuickReply(q.options().get(i), ok, false, round,
                        "Đọc lại mẹo vừa rồi và phần lý thuyết bên trái, xác định đúng dạng bài rồi thay số cẩn thận.",
                        nz(q.explanation()), null, "APPLY"));
            }
            Collections.shuffle(options, rnd);
            options.add(unsure(round));
            msgs.add(AgentReply.ask(kietAsks ? kiet.agentId() : tutor.agentId(), ask, options));
            return new Round(msgs, options);
        }

        Misconception m = misconception(ctx, round);
        Misconception other = otherMisconception(ctx, round);
        String wrong = sentence(stripQuestion(m.wrong()));
        String fix = sentence(firstSentence(m.fix()));
        String otherWrong = other == null ? null : sentence(stripQuestion(other.wrong()));
        String hint = "Chỗ này hay nhầm vì " + lowerFirst(stripEnd(m.why())) + ".";

        if (kind == Kind.TF) {
            boolean claimTrue = rnd.nextBoolean();
            String claim = claimTrue ? fix : wrong;
            msgs.add(AgentReply.say(tutor.agentId(), pick(rnd,
                    "Thầy đố nhanh: câu sau ĐÚNG hay SAI?",
                    "Câu đố của thầy đây: đúng hay sai nào?")));
            List<QuickReply> options = new ArrayList<>();
            if (claimTrue) {
                options.add(opt("Câu đó đúng, vì " + lowerFirst(fix), true, false, round, hint, fix, null, "TF"));
                options.add(opt("Câu đó sai, phải là: " + lowerFirst(wrong), false, false, round, hint, fix, null, "TF"));
            } else {
                options.add(opt("Câu đó sai, phải là: " + lowerFirst(fix), true, false, round, hint, fix, null, "TF"));
                options.add(opt("Câu đó đúng, vì " + lowerFirst(wrong), false, false, round, hint, fix, null, "TF"));
            }
            if (otherWrong != null) {
                options.add(opt("Câu đó sai, phải là: " + lowerFirst(otherWrong), false, false, round, hint, fix, null, "TF"));
            }
            Collections.shuffle(options, rnd);
            options.add(unsure(round));
            msgs.add(AgentReply.ask(tutor.agentId(), "\"" + claim + "\"", options));
            return new Round(msgs, options);
        }

        // CATCH / TRUE / AN_WRONG: một bạn AI phát biểu, học sinh phán xử
        boolean anSpeaks = kind == Kind.AN_WRONG && an != null;
        var speaker = anSpeaks ? an : (kiet != null ? kiet : an);
        if (speaker == null) speaker = tutor;
        boolean claimTrue = kind == Kind.TRUE;
        String who = firstName(speaker.name());
        String claimText = pick(rnd, "Theo mình thì ", "Mình chắc chắn là ", "Dễ mà, ", "Mình nhớ là ", "")
                + lowerFirst(claimTrue ? fix : wrong);
        var claim = claimTrue
                ? AgentReply.say(speaker.agentId(), claimText)
                : AgentReply.mistake(speaker.agentId(), claimText, stripEnd(m.why()) + ". " + m.fix());

        // Người khơi chuyện và người hỏi học sinh đổi ngẫu nhiên
        var other3 = anSpeaks ? kiet : an;
        int intro = rnd.nextInt(3);
        if (intro == 1 && other3 != null) {
            msgs.add(AgentReply.say(other3.agentId(), pick(rnd,
                    who + " ơi, phần này cậu hiểu thế nào?",
                    "Ê " + who + ", cậu giải thích thử phần này đi!")));
        } else if (intro == 2) {
            msgs.add(AgentReply.say(tutor.agentId(), who + ", em nêu thử điều em nhớ về bài này xem nào."));
        }
        msgs.add(claim);

        List<QuickReply> options = new ArrayList<>();
        String explain = claimTrue ? fix : "Đúng phải là: " + lowerFirst(fix);
        String rightText = claimTrue ? who + " nói đúng, vì " + lowerFirst(fix) : who + " sai rồi, đúng phải là: " + lowerFirst(fix);
        String wrongText = claimTrue ? who + " sai rồi, đúng phải là: " + lowerFirst(wrong) : who + " nói đúng, vì " + lowerFirst(wrong);
        String k = claimTrue ? "TRUE" : "CATCH";
        options.add(opt(rightText, true, !claimTrue, round, hint, explain, speaker.agentId(), k));
        options.add(opt(wrongText, false, false, round, hint, explain, speaker.agentId(), k));
        if (otherWrong != null) {
            options.add(opt(who + " sai rồi, đúng phải là: " + lowerFirst(otherWrong), false, false, round, hint, explain, speaker.agentId(), k));
        }
        Collections.shuffle(options, rnd);
        options.add(unsure(round));

        var asker = intro == 1 ? tutor : (other3 != null && rnd.nextBoolean() ? other3 : tutor);
        String ask = pick(rnd,
                student + " thấy " + who + " nói đúng hay sai? Chọn lý do em thấy hợp lý nhất nhé!",
                "Ai đúng ai sai đây? " + student + " phán xử giúp nhé!",
                student + " ơi, cậu có đồng ý với " + who + " không?",
                "Hừm... " + student + " kiểm tra giúp xem câu của " + who + " có chuẩn không nào?");
        msgs.add(AgentReply.ask(asker.agentId(), ask, options));
        return new Round(msgs, options);
    }

    /** Bộ bài các kiểu lượt, xáo theo phiên để mỗi phiên một thứ tự khác */
    private Kind scenario(DiscussionContext ctx, int round) {
        boolean hasM = hasMisconceptions(ctx);
        boolean hasP = ctx.practice() != null && !ctx.practice().isEmpty();
        List<Kind> deck = new ArrayList<>();
        if (hasM) {
            deck.addAll(List.of(Kind.CATCH, Kind.CATCH, Kind.CATCH, Kind.TRUE, Kind.TRUE, Kind.TF));
            if (peer(ctx) != null) deck.add(Kind.AN_WRONG);
        }
        if (hasP) deck.addAll(List.of(Kind.APPLY, Kind.APPLY));
        Collections.shuffle(deck, new Random(Objects.hash(ctx.sessionId(), "deck")));
        return deck.get(Math.floorMod(round, deck.size()));
    }

    private int roundsTotal(DiscussionContext ctx) {
        int m = hasMisconceptions(ctx) ? ctx.misconceptions().size() : 0;
        int p = ctx.practice() == null ? 0 : Math.min(ctx.practice().size(), 3);
        if (m + p == 0) return 0;
        return Math.max(3, m + p);
    }

    private String kindOf(DiscussionContext ctx) {
        return ctx.pendingReplies().stream().map(QuickReply::kind).filter(Objects::nonNull).findFirst().orElse("");
    }

    private String hintMaker(DiscussionContext ctx) {
        return ctx.pendingReplies().stream().map(QuickReply::maker).filter(Objects::nonNull).findFirst().orElse(null);
    }

    private String hintOf(DiscussionContext ctx, int round) {
        return ctx.pendingReplies().stream().map(QuickReply::hint).filter(Objects::nonNull).findFirst()
                .orElseGet(() -> hasMisconceptions(ctx)
                        ? "Chỗ này hay nhầm vì " + lowerFirst(stripEnd(misconception(ctx, round).why())) + "."
                        : "Đọc lại phần lý thuyết bên trái rồi thử lại nhé.");
    }

    // ==================================================================
    //  Gemini: trả lời tự do khi học sinh tự gõ
    // ==================================================================

    private Optional<List<AgentReply>> aiFreeReply(DiscussionContext ctx, String studentMessage) {
        if (!gemini.enabled(GeminiClient.Task.DISCUSSION)) return Optional.empty();
        var tutor = ctx.tutor();
        var kiet = errorMaker(ctx);
        var an = peer(ctx);
        String system = """
                Bạn điều phối 3 nhân vật trong lớp học Toán THCS (Việt Nam):
                - TUTOR: %s, thầy giáo, giảng ngắn gọn từng bước, gợi mở, không giải hộ toàn bộ.
                - KIET: %s, bạn học hay phản biện, vui tính.
                - AN: %s, bạn học nhanh nhạy, thích mẹo tính nhanh.
                Quy tắc BẮT BUỘC:
                1. Chỉ nói về kiến thức của BÀI ĐANG HỌC bên dưới. Học sinh hỏi ngoài lề thì nhẹ nhàng kéo về bài.
                2. Không đưa thông tin sai trong câu trả lời tự do này.
                3. Mỗi câu thoại tối đa 2 câu ngắn, dễ hiểu cho học sinh 11-15 tuổi, xưng hô thân thiện. Không dùng emoji, không dùng LaTeX.
                4. Trả về JSON: {"replies":[{"speaker":"TUTOR|KIET|AN","content":"..."}]} gồm 1-2 câu thoại.
                """.formatted(tutor.name(), kiet != null ? kiet.name() : "Kiệt", an != null ? an.name() : "An");
        String prompt = "BÀI ĐANG HỌC: " + ctx.lessonTitle() + "\nLÝ THUYẾT:\n" + abbreviate(ctx.lessonContent(), 3500)
                + "\n\nLỖI HAY GẶP:\n" + keyPoints(ctx)
                + "\n\nHỘI THOẠI GẦN ĐÂY:\n" + transcript(ctx, 8)
                + "\n\nHỌC SINH (" + ctx.studentName() + ") VỪA NÓI: " + studentMessage;
        Optional<JsonNode> json = gemini.generateJson(GeminiClient.Task.DISCUSSION, system,
                List.of(GeminiClient.Part.text(prompt)), 0.6, 500);
        if (json.isEmpty()) return Optional.empty();
        List<AgentReply> list = new ArrayList<>();
        for (JsonNode r : json.get().path("replies")) {
            String content = r.path("content").asText("").trim();
            if (content.isEmpty()) continue;
            String sp = r.path("speaker").asText("TUTOR");
            var agent = "KIET".equals(sp) && kiet != null ? kiet : "AN".equals(sp) && an != null ? an : tutor;
            list.add(AgentReply.say(agent.agentId(), abbreviate(content, 600)));
            if (list.size() == 2) break;
        }
        return list.isEmpty() ? Optional.empty() : Optional.of(list);
    }

    private static String transcript(DiscussionContext ctx, int last) {
        List<DiscussionContext.Turn> h = ctx.history();
        StringBuilder sb = new StringBuilder();
        for (DiscussionContext.Turn t : h.subList(Math.max(0, h.size() - last), h.size())) {
            sb.append(t.senderName()).append(": ").append(abbreviate(t.content(), 300)).append('\n');
        }
        return sb.toString();
    }

    // ==================================================================
    //  Bài chưa có dữ liệu luyện tập
    // ==================================================================

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

    // ==================================================================
    //  Tiện ích
    // ==================================================================

    private static QuickReply opt(String text, boolean correct, boolean catchError, int round, String hint,
                                  String explain, String maker, String kind) {
        return new QuickReply(text, correct, catchError, round, hint, explain, maker, kind);
    }

    private static QuickReply unsure(int round) {
        return QuickReply.neutral(UNSURE, round);
    }

    /** Xáo lại đáp án khi hỏi lại (giữ nút "chưa chắc" ở cuối) */
    private static List<QuickReply> reshuffle(List<QuickReply> options, Random rnd) {
        List<QuickReply> graded = new ArrayList<>(options.stream().filter(o -> o.correct() != null).toList());
        List<QuickReply> neutral = options.stream().filter(o -> o.correct() == null).toList();
        Collections.shuffle(graded, rnd);
        graded.addAll(neutral);
        return graded;
    }

    private static Random rnd(DiscussionContext ctx, int round, int salt) {
        return new Random(Objects.hash(ctx.sessionId(), round, salt));
    }

    @SafeVarargs
    private static <T> T pick(Random rnd, T... items) {
        return items[rnd.nextInt(items.length)];
    }

    private static boolean hasMisconceptions(DiscussionContext ctx) {
        return ctx.misconceptions() != null && !ctx.misconceptions().isEmpty();
    }

    private static int offset(DiscussionContext ctx, int size) {
        return Math.floorMod(Objects.hash(ctx.sessionId(), "offset"), Math.max(size, 1));
    }

    private static Misconception misconception(DiscussionContext ctx, int round) {
        List<Misconception> list = ctx.misconceptions();
        return list.get(Math.floorMod(round + offset(ctx, list.size()), list.size()));
    }

    private static Misconception otherMisconception(DiscussionContext ctx, int round) {
        List<Misconception> list = ctx.misconceptions();
        if (list.size() < 2) return null;
        int i = Math.floorMod(round + offset(ctx, list.size()), list.size());
        return list.get((i + 1) % list.size());
    }

    private static DiscussionContext.PracticeQuestion practice(DiscussionContext ctx, int round) {
        var list = ctx.practice();
        return list.get(Math.floorMod(round + offset(ctx, list.size()) * 3, list.size()));
    }

    /** Mẹo / công thức Bảo An nêu: dòng có công thức trong lý thuyết, không có thì dùng cách sửa lỗi sai của bài */
    private static String tip(DiscussionContext ctx, int round) {
        String content = ctx.lessonContent() == null ? "" : ctx.lessonContent();
        List<String> formulas = Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .map(l -> l.startsWith("•") ? l.substring(1).trim() : l)
                .filter(l -> l.length() > 12 && l.length() < 160 && (l.contains("=") || l.contains("√") || l.contains("≥")
                        || l.contains("≤") || l.toLowerCase(Locale.ROOT).contains("công thức")))
                .toList();
        // Ưu tiên dòng công thức không phải ví dụ; xen kẽ với cách sửa lỗi sai để mỗi lượt một mẹo khác
        List<String> rules = formulas.stream().filter(l -> !l.toLowerCase(Locale.ROOT).startsWith("ví dụ")).toList();
        List<String> pool = new ArrayList<>(rules.isEmpty() ? formulas : rules);
        if (hasMisconceptions(ctx)) {
            for (Misconception m : ctx.misconceptions()) pool.add(firstSentence(m.fix()));
        }
        if (!pool.isEmpty()) {
            return sentence(pool.get(Math.floorMod(round + offset(ctx, pool.size()), pool.size())));
        }
        return firstSentence(content);
    }

    private static String keyPoints(DiscussionContext ctx) {
        if (!hasMisconceptions(ctx)) return firstSentence(ctx.lessonContent());
        StringBuilder sb = new StringBuilder();
        for (Misconception m : ctx.misconceptions()) {
            sb.append("• ").append(firstSentence(m.fix())).append(' ');
        }
        return sb.toString().trim();
    }

    private static DiscussionContext.AgentInfo agentById(DiscussionContext ctx, String agentId) {
        if (agentId == null) return null;
        return ctx.agents().stream().filter(a -> a.agentId().equals(agentId)).findFirst().orElse(null);
    }

    /** Tuấn Kiệt: CRITIC có persona chứa "cố ý" (mặc định CRITIC đầu tiên) */
    private static DiscussionContext.AgentInfo errorMaker(DiscussionContext ctx) {
        var critics = ctx.critics();
        return critics.stream()
                .filter(a -> a.personaPrompt() != null && a.personaPrompt().toLowerCase(Locale.ROOT).contains("cố ý"))
                .findFirst()
                .orElse(critics.isEmpty() ? null : critics.get(0));
    }

    /** Bảo An: bạn học còn lại */
    private static DiscussionContext.AgentInfo peer(DiscussionContext ctx) {
        var maker = errorMaker(ctx);
        return ctx.critics().stream().filter(a -> a != maker).findFirst().orElse(null);
    }

    private static String firstName(String fullName) {
        if (fullName == null) return "bạn ấy";
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private static String stripQuestion(String s) {
        return s.replaceAll("\\s*(đúng không các bạn\\?|đúng không\\?|nhỉ\\?|mà!|thôi!)\\s*$", "").replaceAll("[!]+$", "").trim();
    }

    private static String stripEnd(String s) {
        return s == null ? "" : s.trim().replaceAll("[.!?…]+$", "");
    }

    /** Một câu ngắn gọn (cắt ở ~130 kí tự theo ranh giới từ), kết thúc bằng dấu chấm */
    private static String sentence(String s) {
        String t = stripEnd(s);
        if (t.length() > MAX_REASON) {
            int cut = t.lastIndexOf(' ', MAX_REASON);
            t = t.substring(0, cut > 40 ? cut : MAX_REASON).replaceAll("[,;:]$", "") + "…";
            return t;
        }
        return t + ".";
    }

    private static String lowerFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        // Giữ chữ hoa nếu là kí hiệu / tên riêng viết tắt (vd "ABC", "A = ...")
        if (s.length() > 1 && Character.isUpperCase(s.charAt(1))) return s;
        if (s.length() > 1 && !Character.isLetter(s.charAt(1))) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
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
        String first = Arrays.stream(content.split("\\R"))
                .map(String::trim)
                .filter(l -> l.length() > 15 && !l.endsWith(":") && !l.startsWith("•"))
                .findFirst()
                .orElse(content.split("\\R")[0].trim().replaceAll(":$", ""));
        int cut = first.indexOf(". ");
        if (cut > 20) first = first.substring(0, cut + 1);
        return first.endsWith(".") || first.endsWith("!") || first.endsWith("?") ? first : first + ".";
    }
}

package com.mael.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mael.dto.game.ChapterNode;
import com.mael.dto.game.MapResponse;
import com.mael.dto.game.StageNode;
import com.mael.dto.insight.InsightView;
import com.mael.entity.Student;
import com.mael.entity.Weakness;
import com.mael.repository.WeaknessRepository;
import com.mael.security.CurrentUser;
import com.mael.service.ai.gemini.GeminiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phân tích cá nhân hoá: đọc kết quả đi ải, sao từng chương, điểm yếu theo chủ đề rồi gợi ý việc nên làm tiếp.
 *
 * - Bật Gemini (key GEMINI_KEY_ANALYSIS): AI viết nhận xét dựa trên số liệu thật của học sinh.
 * - Không có AI: tự tính theo luật (ải ít sao nhất, chương yếu nhất...).
 * Kết quả được nhớ tạm 6 giờ, số liệu thay đổi (qua ải mới) thì phân tích lại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final GameService gameService;
    private final WeaknessRepository weaknessRepository;
    private final CurrentUser currentUser;
    private final GeminiClient gemini;
    private final ObjectMapper objectMapper;

    private record Cached(int hash, InsightView view) {
    }

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public InsightView myInsight(boolean refresh) {
        Student student = currentUser.student();
        MapResponse map = gameService.myMap();
        List<Weakness> weaknesses = weaknessRepository.findByStudentId(student.getId());

        List<InsightView.ChapterSkill> chapters = new ArrayList<>();
        for (ChapterNode c : map.chapters()) {
            int percent = c.starsTotal() == 0 ? 0 : Math.round(c.starsEarned() * 100f / c.starsTotal());
            chapters.add(new InsightView.ChapterSkill(c.topicId(), shortName(c.name()), percent, c.starsEarned(), c.starsTotal()));
        }
        int totalStars = chapters.stream().mapToInt(InsightView.ChapterSkill::stars).sum();
        int totalMax = chapters.stream().mapToInt(InsightView.ChapterSkill::starsTotal).sum();
        int mastery = totalMax == 0 ? 0 : Math.round(totalStars * 100f / totalMax);

        int hash = Objects.hash(totalStars, map.clearedStages(), map.nextStageId(),
                weaknesses.stream().map(w -> w.getTopic().getId() + w.getMasteryScore()).toList());
        Cached c = cache.get(student.getId());
        if (!refresh && c != null && c.hash == hash
                && Duration.between(c.view.generatedAt(), LocalDateTime.now()).compareTo(CACHE_TTL) < 0) {
            return c.view;
        }

        InsightView rule = ruleBased(map, weaknesses, chapters, mastery);
        InsightView result = aiBased(student, map, weaknesses, chapters, mastery, rule).orElse(rule);
        cache.put(student.getId(), new Cached(hash, result));
        return result;
    }

    // ------------------------------------------------------------------

    private InsightView ruleBased(MapResponse map, List<Weakness> weaknesses, List<InsightView.ChapterSkill> chapters, int mastery) {
        List<String> strengths = new ArrayList<>();
        List<InsightView.Focus> focus = new ArrayList<>();
        List<StageNode> played = new ArrayList<>();
        for (ChapterNode c : map.chapters()) {
            for (StageNode s : c.stages()) if ("CLEARED".equals(s.status())) played.add(s);
        }
        long perfect = played.stream().filter(s -> s.stars() == 3).count();
        if (perfect > 0) strengths.add("Đạt 3 sao ở " + perfect + " ải");
        chapters.stream().filter(ch -> ch.percent() >= 80).findFirst()
                .ifPresent(ch -> strengths.add("Vững " + ch.name()));
        if (map.clearedStages() > 0) strengths.add("Đã qua " + map.clearedStages() + "/" + map.totalStages() + " ải");

        if (map.nextStageId() != null) {
            focus.add(new InsightView.Focus("Chơi tiếp: " + map.nextStageTitle(), "Ải tiếp theo trên bản đồ",
                    map.nextStageType(), map.nextStageId()));
        }
        played.stream().filter(s -> s.stars() < 3).min(Comparator.comparingInt(StageNode::stars))
                .ifPresent(s -> focus.add(new InsightView.Focus("Chơi lại: " + s.title(), "Mới đạt " + s.stars() + "/3 sao",
                        "LESSON", s.lessonId())));
        weaknesses.stream().filter(w -> w.getMasteryScore().doubleValue() < 70).findFirst()
                .ifPresent(w -> focus.add(new InsightView.Focus("Luyện Đấu trường " + shortName(w.getTopic().getName()),
                        "Độ thành thạo " + w.getMasteryScore().intValue() + "%", "ARENA", w.getTopic().getId())));

        String headline;
        String mood;
        if (map.clearedStages() == 0) {
            headline = "Bắt đầu hành trình nào! Qua Ải 1 để thầy phân tích chính xác hơn.";
            mood = "START";
        } else if (mastery >= 70) {
            headline = "Em đang học rất tốt, giữ vững phong độ nhé!";
            mood = "HAPPY";
        } else {
            headline = "Em đang tiến bộ! Tập trung vài ải dưới đây để lên tay nhanh hơn.";
            mood = "FOCUS";
        }
        String tip = "Sai câu nào, đọc kỹ lời giải rồi chơi lại ải đó ngay để nhớ lâu hơn.";
        return new InsightView(headline, mood, strengths.stream().limit(3).toList(), focus.stream().limit(3).toList(),
                tip, mastery, chapters, "RULE", LocalDateTime.now());
    }

    private Optional<InsightView> aiBased(Student student, MapResponse map, List<Weakness> weaknesses,
                                          List<InsightView.ChapterSkill> chapters, int mastery, InsightView rule) {
        if (!gemini.enabled(GeminiClient.Task.ANALYSIS) || map.clearedStages() == 0) return Optional.empty();
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("hoc_sinh", Map.of("ten", student.getFullName(), "lop", student.getGradeLevel(),
                    "muc_tieu", student.getLearningGoal() == null ? "" : student.getLearningGoal().name()));
            List<Map<String, Object>> stages = new ArrayList<>();
            for (ChapterNode c : map.chapters()) {
                for (StageNode s : c.stages()) {
                    if ("LOCKED".equals(s.status())) continue;
                    stages.add(Map.of("id", s.lessonId(), "chuong", shortName(c.name()), "ten", s.title(),
                            "sao", s.stars(), "trang_thai", s.status()));
                }
            }
            data.put("ai", stages);
            data.put("chuong", chapters.stream().map(ch -> Map.of("ten", ch.name(), "phan_tram", ch.percent())).toList());
            data.put("diem_yeu", weaknesses.stream().map(w -> Map.of("chu_de", shortName(w.getTopic().getName()),
                    "thanh_thao", w.getMasteryScore().intValue())).toList());
            data.put("ai_tiep_theo", map.nextStageTitle() == null ? "" : map.nextStageTitle());

            String system = """
                    Bạn là Thầy Minh Trí, gia sư Toán THCS. Phân tích kết quả học của học sinh từ dữ liệu JSON.
                    Viết RẤT NGẮN, thân thiện, học sinh 11-15 tuổi đọc trong 10 giây. Không bịa số liệu không có trong dữ liệu.
                    Trả JSON: {"headline":"1 câu <= 20 từ","mood":"HAPPY|FOCUS","strengths":["<= 8 từ", "..."],
                    "focus":[{"title":"<= 8 từ","reason":"<= 10 từ","lessonId":"id ải lấy từ dữ liệu"}],"tip":"1 mẹo học <= 20 từ"}
                    strengths tối đa 3 ý, focus tối đa 3 ý, ưu tiên ải ít sao và chương phần trăm thấp.
                    """;
            Optional<JsonNode> json = gemini.generateJson(GeminiClient.Task.ANALYSIS, system,
                    List.of(GeminiClient.Part.text(objectMapper.writeValueAsString(data))), 0.5, 600);
            if (json.isEmpty()) return Optional.empty();
            JsonNode j = json.get();
            Set<String> validIds = new HashSet<>();
            stages.forEach(s -> validIds.add((String) s.get("id")));
            List<String> strengths = new ArrayList<>();
            j.path("strengths").forEach(s -> { if (strengths.size() < 3 && !s.asText().isBlank()) strengths.add(s.asText()); });
            List<InsightView.Focus> focus = new ArrayList<>();
            for (JsonNode f : j.path("focus")) {
                String id = f.path("lessonId").asText("");
                if (!validIds.contains(id) || focus.size() >= 3) continue;
                focus.add(new InsightView.Focus(f.path("title").asText(), f.path("reason").asText(), "LESSON", id));
            }
            if (focus.isEmpty()) focus.addAll(rule.focus());
            String mood = "HAPPY".equals(j.path("mood").asText()) ? "HAPPY" : "FOCUS";
            return Optional.of(new InsightView(j.path("headline").asText(rule.headline()), mood,
                    strengths.isEmpty() ? rule.strengths() : strengths, focus,
                    j.path("tip").asText(rule.tip()), mastery, chapters, "AI", LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("Phân tích AI lỗi: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** "Chương I. Phương trình..." -> "Phương trình..." */
    private static String shortName(String name) {
        int dot = name.indexOf(". ");
        return name.startsWith("Chương") && dot > 0 ? name.substring(dot + 2) : name;
    }
}

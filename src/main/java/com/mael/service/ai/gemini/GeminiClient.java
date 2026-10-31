package com.mael.service.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mael.service.ai.motapi.MotApiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gọi Gemini API (generateContent). Mỗi tác vụ dùng 1 API key riêng để chia tải:
 *
 * <pre>
 *  DISCUSSION  GEMINI_KEY_DISCUSSION  3 AI trả lời khi học sinh tự gõ câu hỏi, tóm tắt buổi thảo luận
 *  ANALYSIS    GEMINI_KEY_ANALYSIS    phân tích cá nhân hoá điểm mạnh / yếu của học sinh
 *  DOCUMENT    GEMINI_KEY_DOCUMENT    đọc PDF giảng viên tải lên, soạn câu hỏi + mini game
 *  PHOTO       GEMINI_KEY_PHOTO       giải bài toán từ ảnh chụp
 * </pre>
 *
 * Key của tác vụ bị hết lượt (429) hoặc lỗi máy chủ thì thử lần lượt các key còn lại.
 * Model mặc định là bản Flash-Lite (rẻ, nhanh); model chính không tồn tại (404) thì dùng model dự phòng.
 * Không có key nào hoặc AI_PROVIDER khác gemini: {@link #enabled(Task)} = false, hệ thống dùng kịch bản soạn sẵn.
 *
 * AI_PROVIDER=gemini,motapis: tác vụ thuần chữ (mặc định DISCUSSION, ANALYSIS) gọi DeepSeek qua {@link MotApiClient} trước,
 * DeepSeek lỗi thì gọi Gemini. Tác vụ có ảnh / PDF luôn gọi Gemini.
 */
@Slf4j
@Component
public class GeminiClient {

    public enum Task { DISCUSSION, ANALYSIS, DOCUMENT, PHOTO }

    /** Một phần nội dung gửi cho Gemini: chữ hoặc file (ảnh / PDF) dạng base64 */
    public record Part(String text, String mimeType, String base64Data) {
        public static Part text(String text) {
            return new Part(text, null, null);
        }

        public static Part file(String mimeType, byte[] data) {
            return new Part(null, mimeType, Base64.getEncoder().encodeToString(data));
        }
    }

    private final boolean providerOn;
    private final String model;
    private final String fallbackModel;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final Map<Task, String> keys = new EnumMap<>(Task.class);
    private final MotApiClient motApi;

    public GeminiClient(
            @Value("${app.ai.provider:mock}") String provider,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.gemini.model:gemini-3.1-flash-lite}") String model,
            @Value("${app.gemini.fallback-model:gemini-2.5-flash-lite}") String fallbackModel,
            @Value("${app.gemini.timeout-seconds:60}") int timeoutSeconds,
            @Value("${app.gemini.keys.discussion:}") String keyDiscussion,
            @Value("${app.gemini.keys.analysis:}") String keyAnalysis,
            @Value("${app.gemini.keys.document:}") String keyDocument,
            @Value("${app.gemini.keys.photo:}") String keyPhoto,
            ObjectMapper objectMapper,
            MotApiClient motApi) {
        this.providerOn = provider.toLowerCase(Locale.ROOT).contains("gemini");
        this.motApi = motApi;
        this.model = model.trim();
        this.fallbackModel = fallbackModel.trim();
        this.objectMapper = objectMapper;
        keys.put(Task.DISCUSSION, keyDiscussion.trim());
        keys.put(Task.ANALYSIS, keyAnalysis.trim());
        keys.put(Task.DOCUMENT, keyDocument.trim());
        keys.put(Task.PHOTO, keyPhoto.trim());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        if (providerOn) {
            long count = keys.values().stream().filter(k -> !k.isBlank()).count();
            log.info("Gemini bật: model {} (dự phòng {}), {} / 4 key được cấu hình", this.model, this.fallbackModel, count);
        }
    }

    /** Có gọi được AI cho tác vụ này không (Gemini có key, hoặc DeepSeek nhận tác vụ này) */
    public boolean enabled(Task task) {
        return geminiEnabled(task) || motApi.handles(task);
    }

    private boolean geminiEnabled(Task task) {
        return providerOn && !candidateKeys(task).isEmpty();
    }

    public String modelName() {
        return model;
    }

    /** Tên model sẽ trả lời tác vụ này (để lưu vào generatedBy) */
    public String modelName(Task task) {
        return motApi.handles(task) ? motApi.modelName() : model;
    }

    /**
     * Gọi Gemini, yêu cầu trả JSON. Trả về Optional.empty() nếu tắt AI hoặc mọi key đều lỗi
     * (nơi gọi tự dùng kịch bản dự phòng).
     */
    public Optional<JsonNode> generateJson(Task task, String systemPrompt, List<Part> parts, double temperature, int maxTokens) {
        return generate(task, systemPrompt, parts, temperature, maxTokens, true).flatMap(text -> {
            try {
                return Optional.of(objectMapper.readTree(stripFence(text)));
            } catch (Exception e) {
                log.warn("Gemini {} trả JSON không hợp lệ: {}", task, abbreviate(text));
                return Optional.empty();
            }
        });
    }

    public Optional<String> generateText(Task task, String systemPrompt, List<Part> parts, double temperature, int maxTokens) {
        return generate(task, systemPrompt, parts, temperature, maxTokens, false);
    }

    // ------------------------------------------------------------------

    private Optional<String> generate(Task task, String systemPrompt, List<Part> parts, double temperature,
                                      int maxTokens, boolean json) {
        if (!enabled(task)) return Optional.empty();
        boolean hasFile = parts.stream().anyMatch(p -> p.base64Data() != null);
        if (!hasFile && motApi.handles(task)) {
            String userText = parts.stream().map(Part::text).filter(Objects::nonNull).collect(Collectors.joining("\n\n"));
            Optional<String> deepseek = motApi.chat(task, systemPrompt, userText, temperature, maxTokens, json);
            if (deepseek.isPresent()) return deepseek;
            log.info("DeepSeek {} không trả lời được, chuyển sang Gemini", task);
        }
        if (!geminiEnabled(task)) return Optional.empty();
        byte[] body;
        try {
            // Gửi dạng byte có Content-Length rõ ràng (không dùng chunked)
            body = objectMapper.writeValueAsBytes(buildBody(systemPrompt, parts, temperature, maxTokens, json));
        } catch (Exception e) {
            return Optional.empty();
        }
        for (String key : candidateKeys(task)) {
            for (String m : models()) {
                try {
                    JsonNode res = restClient.post()
                            .uri("/v1beta/models/{model}:generateContent", m)
                            .header("x-goog-api-key", key)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(body)
                            .retrieve()
                            .body(JsonNode.class);
                    String text = extractText(res);
                    if (text != null && !text.isBlank()) return Optional.of(text.trim());
                    log.warn("Gemini {} trả về rỗng (model {}): {}", task, m, abbreviate(String.valueOf(res)));
                    break;
                } catch (RestClientResponseException e) {
                    int status = e.getStatusCode().value();
                    log.warn("Gemini {} lỗi {} với model {}: {}", task, status, m, abbreviate(e.getResponseBodyAsString()));
                    if (status == 404) continue;          // model không có: thử model dự phòng
                    break;                               // 429 / 401 / 5xx: đổi sang key khác
                } catch (Exception e) {
                    log.warn("Gemini {} không gọi được: {}", task, e.getMessage());
                    break;
                }
            }
        }
        return Optional.empty();
    }

    private ObjectNode buildBody(String systemPrompt, List<Part> parts, double temperature, int maxTokens, boolean json) {
        ObjectNode body = objectMapper.createObjectNode();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.putObject("systemInstruction").putArray("parts").addObject().put("text", systemPrompt);
        }
        ObjectNode content = body.putArray("contents").addObject();
        content.put("role", "user");
        ArrayNode arr = content.putArray("parts");
        for (Part p : parts) {
            if (p.text() != null) {
                arr.addObject().put("text", p.text());
            } else if (p.base64Data() != null) {
                ObjectNode inline = arr.addObject().putObject("inlineData");
                inline.put("mimeType", p.mimeType());
                inline.put("data", p.base64Data());
            }
        }
        ObjectNode cfg = body.putObject("generationConfig");
        cfg.put("temperature", temperature);
        cfg.put("maxOutputTokens", maxTokens);
        if (json) cfg.put("responseMimeType", "application/json");
        return body;
    }

    private static String extractText(JsonNode res) {
        if (res == null) return null;
        JsonNode parts = res.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) return null;
        StringBuilder sb = new StringBuilder();
        for (JsonNode p : parts) {
            if (p.path("thought").asBoolean(false)) continue;
            if (p.hasNonNull("text")) sb.append(p.get("text").asText());
        }
        return sb.toString();
    }

    /** Key của tác vụ trước, sau đó các key còn lại (không trùng) */
    private List<String> candidateKeys(Task task) {
        LinkedHashSet<String> list = new LinkedHashSet<>();
        String own = keys.get(task);
        if (own != null && !own.isBlank()) list.add(own);
        for (Task t : Task.values()) {
            String k = keys.get(t);
            if (k != null && !k.isBlank()) list.add(k);
        }
        return new ArrayList<>(list);
    }

    private List<String> models() {
        return fallbackModel.isBlank() || fallbackModel.equals(model) ? List.of(model) : List.of(model, fallbackModel);
    }

    private static String stripFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?\\s*", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) t = t.substring(0, end);
        }
        return t.trim();
    }

    private static String abbreviate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}

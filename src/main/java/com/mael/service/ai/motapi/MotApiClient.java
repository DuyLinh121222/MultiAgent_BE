package com.mael.service.ai.motapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mael.service.ai.gemini.GeminiClient.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Gọi DeepSeek (deepseek-v4-flash) qua MotAPI, chuẩn OpenAI Chat Completions: POST {base-url}/chat/completions.
 *
 * DeepSeek chỉ đọc được chữ (không đọc ảnh / PDF), nên chỉ dùng cho tác vụ thuần văn bản:
 * mặc định DISCUSSION (3 AI thảo luận) và ANALYSIS (phân tích điểm mạnh / yếu).
 * PHOTO và DOCUMENT luôn để Gemini. Gọi lỗi thì {@link com.mael.service.ai.gemini.GeminiClient} tự chuyển sang Gemini.
 *
 * Bật khi AI_PROVIDER có chữ "motapis" (VD: gemini,motapis) và có MOTAPIS_API_KEY.
 */
@Slf4j
@Component
public class MotApiClient {

    private final boolean on;
    private final String apiKey;
    private final String model;
    private final Set<Task> tasks = EnumSet.noneOf(Task.class);
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public MotApiClient(
            @Value("${app.ai.provider:mock}") String provider,
            @Value("${app.motapis.base-url:https://motapis.com/v1}") String baseUrl,
            @Value("${app.motapis.api-key:}") String apiKey,
            @Value("${app.motapis.model:deepseek-v4-flash}") String model,
            @Value("${app.motapis.tasks:discussion,analysis}") String taskList,
            @Value("${app.motapis.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model.trim();
        this.objectMapper = objectMapper;
        this.on = provider.toLowerCase(Locale.ROOT).contains("motapis") && !this.apiKey.isEmpty();
        Arrays.stream(taskList.split(","))
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .forEach(s -> {
                    try {
                        Task t = Task.valueOf(s);
                        // Ảnh và PDF cần model đọc được file: không cho DeepSeek làm
                        if (t != Task.PHOTO && t != Task.DOCUMENT) tasks.add(t);
                    } catch (IllegalArgumentException e) {
                        log.warn("MOTAPIS_TASKS có tác vụ không hợp lệ: {}", s);
                    }
                });
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(timeoutSeconds * 1000);
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.restClient = RestClient.builder().baseUrl(base).requestFactory(factory).build();
        if (on) log.info("MotAPI bật: model {} cho tác vụ {}", this.model, tasks);
    }

    /** DeepSeek có nhận tác vụ này không */
    public boolean handles(Task task) {
        return on && tasks.contains(task);
    }

    public String modelName() {
        return model;
    }

    /**
     * Gọi chat completions. Optional.empty() khi lỗi (429, hết quota, mạng...): nơi gọi chuyển sang Gemini.
     */
    public Optional<String> chat(Task task, String systemPrompt, String userText, double temperature,
                                 int maxTokens, boolean json) {
        if (!handles(task)) return Optional.empty();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        String system = systemPrompt == null ? "" : systemPrompt;
        // OpenAI json_object yêu cầu trong prompt có chữ "json"
        if (json && !system.toLowerCase(Locale.ROOT).contains("json")) {
            system = (system + "\nChỉ trả về một đối tượng JSON hợp lệ.").trim();
        }
        if (!system.isBlank()) messages.addObject().put("role", "system").put("content", system);
        messages.addObject().put("role", "user").put("content", userText);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        if (json) body.putObject("response_format").put("type", "json_object");

        long start = System.currentTimeMillis();
        try {
            JsonNode res = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(objectMapper.writeValueAsBytes(body))
                    .retrieve()
                    .body(JsonNode.class);
            String text = res == null ? null
                    : res.path("choices").path(0).path("message").path("content").asText(null);
            long ms = System.currentTimeMillis() - start;
            if (text == null || text.isBlank()) {
                log.warn("MotAPI {} trả về rỗng sau {} ms", task, ms);
                return Optional.empty();
            }
            log.debug("MotAPI {} ok ({} ms, model {})", task, ms, res.path("model").asText(model));
            return Optional.of(text.trim());
        } catch (RestClientResponseException e) {
            // Không in API key; chỉ in mã lỗi + một đoạn thông báo
            String msg = e.getResponseBodyAsString();
            log.warn("MotAPI {} lỗi HTTP {}: {}", task, e.getStatusCode().value(),
                    msg.length() > 300 ? msg.substring(0, 300) + "..." : msg);
        } catch (Exception e) {
            log.warn("MotAPI {} không gọi được: {}", task, e.getMessage());
        }
        return Optional.empty();
    }
}

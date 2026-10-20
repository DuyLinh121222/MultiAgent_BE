package com.mael.service.tts;

import com.mael.entity.AiAgent;
import com.mael.exception.ApiException;
import com.mael.repository.AiAgentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đọc tin nhắn của AI bằng Google Cloud Text-to-Speech (REST + API key), mỗi AI một giọng riêng
 * theo AI_AGENT.voice_name / voice_pitch / voice_rate.
 *
 * Chưa có GOOGLE_TTS_API_KEY thì enabled() = false, FE tự dùng giọng đọc có sẵn của trình duyệt.
 * Bật API: Google Cloud Console → APIs & Services → bật "Cloud Text-to-Speech API" → Credentials → tạo API key.
 */
@Slf4j
@Service
public class TtsService {

    private static final int MAX_CHARS = 1500;
    private static final int CACHE_SIZE = 300;

    private final AiAgentRepository agentRepository;
    private final RestClient restClient;
    private final String apiKey;

    /** Cache LRU: cùng 1 câu của cùng 1 AI thì không gọi Google lần nữa */
    private final Map<String, byte[]> cache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    public TtsService(AiAgentRepository agentRepository,
                      RestClient.Builder builder,
                      @Value("${app.google-tts.api-key:}") String apiKey,
                      @Value("${app.google-tts.base-url:https://texttospeech.googleapis.com/v1}") String baseUrl) {
        this.agentRepository = agentRepository;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public boolean enabled() {
        return !apiKey.isEmpty();
    }

    /**
     * Trả về file MP3. Văn bản nên được FE chuẩn hoá trước (√ → "căn", ² → "bình phương"...).
     */
    public byte[] synthesize(String agentId, String text) {
        if (!enabled()) {
            throw ApiException.badRequest("Chưa cấu hình GOOGLE_TTS_API_KEY");
        }
        if (text == null || text.isBlank()) {
            throw ApiException.badRequest("Thiếu nội dung cần đọc");
        }
        String input = text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
        AiAgent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy AI"));

        String key = agentId + "|" + input;
        synchronized (cache) {
            byte[] hit = cache.get(key);
            if (hit != null) return hit;
        }

        Map<String, Object> body = Map.of(
                "input", Map.of("text", input),
                "voice", Map.of("languageCode", "vi-VN",
                        "name", agent.getVoiceName() != null ? agent.getVoiceName() : "vi-VN-Wavenet-A"),
                "audioConfig", Map.of("audioEncoding", "MP3",
                        "pitch", toDouble(agent.getVoicePitch(), 0),
                        "speakingRate", toDouble(agent.getVoiceRate(), 1)));
        try {
            TtsResponse res = restClient.post()
                    .uri(uri -> uri.path("/text:synthesize").queryParam("key", apiKey).build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TtsResponse.class);
            if (res == null || res.audioContent() == null) {
                throw ApiException.badRequest("Google Text-to-Speech không trả về âm thanh");
            }
            byte[] audio = Base64.getDecoder().decode(res.audioContent());
            synchronized (cache) {
                cache.put(key, audio);
            }
            return audio;
        } catch (RestClientException e) {
            log.warn("Gọi Google Text-to-Speech lỗi: {}", e.getMessage());
            throw ApiException.badRequest("Không tạo được giọng đọc, thử lại sau");
        }
    }

    /** Cấu hình giọng của các AI để FE dùng khi đọc bằng giọng trình duyệt */
    public List<VoiceInfo> voices() {
        return agentRepository.findAll().stream()
                .map(a -> new VoiceInfo(a.getId(), a.getName(), a.getVoiceName(),
                        toDouble(a.getVoicePitch(), 0), toDouble(a.getVoiceRate(), 1)))
                .toList();
    }

    private static double toDouble(BigDecimal v, double fallback) {
        return v == null ? fallback : v.doubleValue();
    }

    record TtsResponse(String audioContent) {
    }

    public record VoiceInfo(String agentId, String name, String voiceName, double pitch, double rate) {
    }
}

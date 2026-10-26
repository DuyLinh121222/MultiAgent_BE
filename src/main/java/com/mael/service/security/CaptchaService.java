package com.mael.service.security;

import com.mael.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Captcha hình (SVG) chống gửi form tự động: mã 5 kí tự, dùng 1 lần, hết hạn sau 5 phút.
 * Dùng cho form đăng ký học sinh và form tư vấn trung tâm / trường học.
 */
@Service
public class CaptchaService {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final RateLimiter rateLimiter;
    private final boolean enabled;

    private record Entry(String code, long expiresAt) {
    }

    public record Captcha(String token, String image, boolean enabled) {
    }

    public CaptchaService(RateLimiter rateLimiter, @Value("${app.security.captcha-enabled:true}") boolean enabled) {
        this.rateLimiter = rateLimiter;
        this.enabled = enabled;
    }

    public Captcha create(String ip) {
        if (!enabled) return new Captcha(null, null, false);
        if (!rateLimiter.tryAcquire("captcha:" + ip, 40, Duration.ofMinutes(10))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "Bạn tải mã xác nhận quá nhiều lần, thử lại sau ít phút");
        }
        long now = System.currentTimeMillis();
        if (store.size() > 5000) store.entrySet().removeIf(e -> e.getValue().expiresAt < now);
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 5; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        String token = UUID.randomUUID().toString();
        store.put(token, new Entry(code.toString(), now + TTL.toMillis()));
        return new Captcha(token, image(code.toString()), true);
    }

    /** Kiểm tra mã (dùng 1 lần). Sai thì báo lỗi 400 */
    public void verify(String token, String answer) {
        if (!enabled) return;
        if (token == null || token.isBlank() || answer == null || answer.isBlank()) {
            throw ApiException.badRequest("Vui lòng nhập mã xác nhận");
        }
        Entry e = store.remove(token);
        if (e == null || e.expiresAt < System.currentTimeMillis()) {
            throw ApiException.badRequest("Mã xác nhận đã hết hạn, bấm đổi mã khác nhé");
        }
        if (!e.code.equalsIgnoreCase(answer.trim().replace(" ", ""))) {
            throw ApiException.badRequest("Mã xác nhận chưa đúng, thử lại với mã mới nhé");
        }
    }

    public boolean enabled() {
        return enabled;
    }

    /** Ảnh PNG (khó đọc tự động hơn SVG chữ). Máy không vẽ được ảnh thì dùng SVG dự phòng */
    private String image(String code) {
        try {
            int w = 170, h = 60;
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = img.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new java.awt.Color(0xEE, 0xF3, 0xFF));
            g.fillRect(0, 0, w, h);
            java.awt.Color[] colors = {new java.awt.Color(0x004AC6), new java.awt.Color(0x855300), new java.awt.Color(0x006242),
                    new java.awt.Color(0xA50064), new java.awt.Color(0x3B3F8F)};
            for (int i = 0; i < 40; i++) {
                g.setColor(new java.awt.Color(115, 118, 134, 120));
                g.fillOval(random.nextInt(w), random.nextInt(h), 2, 2);
            }
            for (int i = 0; i < code.length(); i++) {
                java.awt.geom.AffineTransform old = g.getTransform();
                int x = 14 + i * 30 + random.nextInt(6);
                int y = 40 + random.nextInt(10) - 5;
                g.rotate(Math.toRadians(random.nextInt(50) - 25), x + 10, y - 10);
                g.setFont(new java.awt.Font(random.nextBoolean() ? java.awt.Font.SANS_SERIF : java.awt.Font.SERIF,
                        java.awt.Font.BOLD, 28 + random.nextInt(6)));
                g.setColor(colors[random.nextInt(colors.length)]);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.setTransform(old);
            }
            g.setStroke(new java.awt.BasicStroke(2f));
            for (int i = 0; i < 5; i++) {
                g.setColor(colors[random.nextInt(colors.length)]);
                java.awt.geom.QuadCurve2D c = new java.awt.geom.QuadCurve2D.Float(0, random.nextInt(h), random.nextInt(w),
                        random.nextInt(h), w, random.nextInt(h));
                g.draw(c);
            }
            g.dispose();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(img, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Throwable e) {
            String svg = svg(code);
            return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String svg(String code) {
        int w = 160, h = 56;
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(w).append("' height='").append(h)
                .append("' viewBox='0 0 ").append(w).append(' ').append(h).append("'>");
        sb.append("<rect width='100%' height='100%' rx='12' fill='#eef3ff'/>");
        String[] colors = {"#004ac6", "#855300", "#006242", "#a50064", "#3b3f8f"};
        for (int i = 0; i < 6; i++) {
            sb.append("<path d='M").append(random.nextInt(w)).append(' ').append(random.nextInt(h))
                    .append(" Q").append(random.nextInt(w)).append(' ').append(random.nextInt(h))
                    .append(' ').append(random.nextInt(w)).append(' ').append(random.nextInt(h))
                    .append("' stroke='").append(colors[random.nextInt(colors.length)]).append("' stroke-opacity='0.35' stroke-width='")
                    .append(1 + random.nextInt(2)).append("' fill='none'/>");
        }
        for (int i = 0; i < 30; i++) {
            sb.append("<circle cx='").append(random.nextInt(w)).append("' cy='").append(random.nextInt(h))
                    .append("' r='1.2' fill='#737686' fill-opacity='0.5'/>");
        }
        for (int i = 0; i < code.length(); i++) {
            int x = 18 + i * 27 + random.nextInt(6);
            int y = 38 + random.nextInt(8) - 4;
            int rot = random.nextInt(50) - 25;
            sb.append("<text x='").append(x).append("' y='").append(y).append("' transform='rotate(").append(rot)
                    .append(' ').append(x).append(' ').append(y).append(")' font-family='Verdana,Arial,sans-serif' font-size='")
                    .append(26 + random.nextInt(6)).append("' font-weight='bold' fill='")
                    .append(colors[random.nextInt(colors.length)]).append("'>").append(code.charAt(i)).append("</text>");
        }
        sb.append("</svg>");
        return sb.toString();
    }
}

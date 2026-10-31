package com.mael.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * Upload ảnh lên Cloudflare R2 (API tương thích S3), tự ký AWS Signature V4 nên không cần thêm thư viện AWS SDK.
 *
 * Cấu hình trong .env:
 * <pre>
 *  R2_ACCOUNT_ID         Account ID (Cloudflare Dashboard → R2 → bên phải trang)
 *  R2_ACCESS_KEY_ID      R2 → Manage API tokens → Create API token (quyền Object Read & Write)
 *  R2_SECRET_ACCESS_KEY
 *  R2_BUCKET             tên bucket, VD mael-images
 *  R2_PUBLIC_URL         URL public của bucket: https://pub-xxxx.r2.dev hoặc domain riêng (không có / ở cuối)
 * </pre>
 * Thiếu 1 trong 5 biến thì {@link #enabled()} = false, ảnh lưu ở ổ đĩa server như cũ.
 */
@Slf4j
@Component
public class R2Storage {

    private static final DateTimeFormatter AMZ_DATE = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REGION = "auto";
    private static final String SERVICE = "s3";

    private final String accountId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String bucket;
    private final String publicUrl;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public R2Storage(@Value("${app.r2.account-id:}") String accountId,
                     @Value("${app.r2.access-key-id:}") String accessKeyId,
                     @Value("${app.r2.secret-access-key:}") String secretAccessKey,
                     @Value("${app.r2.bucket:}") String bucket,
                     @Value("${app.r2.public-url:}") String publicUrl) {
        this.accountId = accountId.trim();
        this.accessKeyId = accessKeyId.trim();
        this.secretAccessKey = secretAccessKey.trim();
        this.bucket = bucket.trim();
        String pub = publicUrl.trim();
        this.publicUrl = pub.endsWith("/") ? pub.substring(0, pub.length() - 1) : pub;
        if (enabled()) log.info("Cloudflare R2 bật: bucket {}", this.bucket);
    }

    public boolean enabled() {
        return !accountId.isEmpty() && !accessKeyId.isEmpty() && !secretAccessKey.isEmpty()
                && !bucket.isEmpty() && !publicUrl.isEmpty();
    }

    /**
     * Upload file, trả về URL public để lưu vào DB.
     *
     * @param key đường dẫn trong bucket, chỉ dùng chữ, số, - _ . / (VD photo-problems/2026/10/uuid.jpg)
     */
    public String put(String key, byte[] data, String contentType) {
        if (!key.matches("[A-Za-z0-9._/-]+")) throw new IllegalArgumentException("Key R2 không hợp lệ: " + key);
        String host = accountId + ".r2.cloudflarestorage.com";
        String path = "/" + bucket + "/" + key;
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String authorization;
        String amzDate = AMZ_DATE.format(now);
        String payloadHash = hex(sha256(data));
        try {
            authorization = sign("PUT", host, path, amzDate, DATE_STAMP.format(now), payloadHash);
        } catch (Exception e) {
            throw new IllegalStateException("Không ký được request R2", e);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create("https://" + host + path))
                .timeout(Duration.ofSeconds(60))
                .header("Authorization", authorization)
                .header("x-amz-date", amzDate)
                .header("x-amz-content-sha256", payloadHash)
                .header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();
        try {
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                String body = res.body();
                log.warn("R2 upload lỗi HTTP {}: {}", res.statusCode(), body.length() > 300 ? body.substring(0, 300) : body);
                throw new IllegalStateException("R2 trả lỗi HTTP " + res.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Upload R2 bị gián đoạn", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Không kết nối được R2: " + e.getMessage(), e);
        }
        return publicUrl + "/" + key;
    }

    /** Header Authorization theo AWS Signature V4 (chỉ ký host + x-amz-content-sha256 + x-amz-date) */
    String sign(String method, String host, String path, String amzDate, String dateStamp, String payloadHash) throws Exception {
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";
        String canonicalRequest = method + "\n"
                + path + "\n"
                + "\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:" + amzDate + "\n"
                + "\n"
                + signedHeaders + "\n"
                + payloadHash;
        String scope = dateStamp + "/" + REGION + "/" + SERVICE + "/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + scope + "\n"
                + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));
        byte[] kDate = hmac(("AWS4" + secretAccessKey).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] kRegion = hmac(kDate, REGION);
        byte[] kService = hmac(kRegion, SERVICE);
        byte[] kSigning = hmac(kService, "aws4_request");
        String signature = hex(hmac(kSigning, stringToSign));
        return "AWS4-HMAC-SHA256 Credential=" + accessKeyId + "/" + scope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private static byte[] hmac(byte[] key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }
}

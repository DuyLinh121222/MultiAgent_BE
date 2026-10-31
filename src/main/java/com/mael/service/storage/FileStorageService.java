package com.mael.service.storage;

import com.mael.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Lưu file. Ảnh: lên Cloudflare R2 nếu đã cấu hình (URL https://...), chưa cấu hình thì lưu ổ đĩa local (/uploads/xxx).
 * PDF tài liệu: vẫn lưu ổ đĩa local (thư mục app.upload.dir).
 */
@Service
public class FileStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/heic");

    private final Path root;
    private final R2Storage r2;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir, R2Storage r2) throws IOException {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        this.r2 = r2;
        Files.createDirectories(root);
    }

    /** Ảnh đã kiểm tra hợp lệ, đọc sẵn vào bộ nhớ (chưa lưu đi đâu) */
    public record ImageData(byte[] bytes, String mimeType, String extension) {
    }

    /** Kiểm tra định dạng + đọc ảnh vào bộ nhớ. Chưa lưu: để AI kiểm tra trước, ảnh không hợp lệ thì khỏi tốn chỗ */
    public ImageData readImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn ảnh");
        }
        if (file.getContentType() == null || !IMAGE_TYPES.contains(file.getContentType())) {
            throw ApiException.badRequest("Chỉ hỗ trợ ảnh JPG, PNG, WEBP hoặc HEIC");
        }
        String ext = switch (file.getContentType()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/heic" -> ".heic";
            default -> ".jpg";
        };
        try {
            return new ImageData(file.getBytes(), file.getContentType(), ext);
        } catch (IOException e) {
            throw ApiException.badRequest("Không đọc được ảnh, thử chọn lại");
        }
    }

    /**
     * Lưu ảnh, trả về URL để ghi vào DB. Có R2: https://pub-xxx.r2.dev/folder/yyyy/MM/uuid.jpg.
     * Chưa có R2: /uploads/folder/uuid.jpg trên ổ đĩa server (Render xoá ổ đĩa mỗi lần deploy).
     */
    public String saveImage(ImageData image, String folder) {
        String name = UUID.randomUUID() + image.extension();
        if (r2.enabled()) {
            String month = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM"));
            try {
                return r2.put(folder + "/" + month + "/" + name, image.bytes(), image.mimeType());
            } catch (RuntimeException e) {
                throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Không lưu được ảnh, thử lại sau");
            }
        }
        try {
            Path dir = root.resolve(folder).normalize();
            Files.createDirectories(dir);
            Files.write(dir.resolve(name), image.bytes());
            return "/uploads/" + folder + "/" + name;
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được ảnh");
        }
    }

    /** Lưu file PDF tài liệu (kiểm tra cả đuôi, content-type và 5 byte đầu "%PDF-") */
    public StoredFile storePdf(MultipartFile file, String folder, long maxBytes) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Vui lòng chọn file PDF");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".pdf")) {
            throw ApiException.badRequest("Chỉ nhận file PDF (.pdf)");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest("File PDF tối đa " + (maxBytes / 1024 / 1024) + "MB");
        }
        try {
            byte[] head = new byte[5];
            try (InputStream in = file.getInputStream()) {
                if (in.read(head) != 5 || !new String(head, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
                    throw ApiException.badRequest("File không phải PDF hợp lệ");
                }
            }
            Path dir = root.resolve(folder).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + ".pdf");
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile("/uploads/" + folder + "/" + target.getFileName(), target);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được file");
        }
    }

    /** Đường dẫn trên ổ đĩa của URL /uploads/... */
    public Path resolveUrl(String url) {
        String rel = url.startsWith("/uploads/") ? url.substring("/uploads/".length()) : url;
        Path p = root.resolve(rel).normalize();
        if (!p.startsWith(root)) throw ApiException.badRequest("Đường dẫn file không hợp lệ");
        return p;
    }

    /**
     * @param url  đường dẫn public (lưu vào DB)
     * @param path đường dẫn file trên ổ đĩa (để gửi cho AI đọc ảnh)
     */
    public record StoredFile(String url, Path path) {
    }
}

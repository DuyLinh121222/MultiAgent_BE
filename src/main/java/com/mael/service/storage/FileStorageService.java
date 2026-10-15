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
 * Lưu file lên ổ đĩa local (thư mục app.upload.dir), trả về URL dạng /uploads/xxx.
 * Khi tích hợp Cloudinary: thay phần ghi file bằng Cloudinary upload, trả về secure_url.
 */
@Service
public class FileStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/heic");

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) throws IOException {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public StoredFile storeImage(MultipartFile file, String folder) {
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
        String name = UUID.randomUUID() + ext;
        try {
            Path dir = root.resolve(folder).normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile("/uploads/" + folder + "/" + name, target);
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Không lưu được ảnh");
        }
    }

    /**
     * @param url  đường dẫn public (lưu vào DB)
     * @param path đường dẫn file trên ổ đĩa (để gửi cho AI đọc ảnh)
     */
    public record StoredFile(String url, Path path) {
    }
}

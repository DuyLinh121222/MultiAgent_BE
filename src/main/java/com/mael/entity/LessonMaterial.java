package com.mael.entity;

import com.mael.entity.enums.FileType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonMaterial {

    @Id
    @UuidGenerator
    @Column(name = "material_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_url", nullable = false, length = 1000)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 20)
    private FileType fileType;

    @Column(name = "file_size")
    private Long fileSize;

    /** Giảng viên tải tài liệu lên */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Lecturer uploadedBy;

    /** Trạng thái AI đọc tài liệu: NONE | PROCESSING | DONE | FAILED */
    @Column(name = "ai_status", nullable = false, length = 20)
    @Builder.Default
    private String aiStatus = "NONE";

    /** Kết quả / lỗi khi AI đọc tài liệu (hiện cho giảng viên) */
    @Column(name = "ai_note", columnDefinition = "TEXT")
    private String aiNote;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}

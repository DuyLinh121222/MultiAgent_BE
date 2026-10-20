package com.mael.entity;

import com.mael.entity.enums.SenderType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tin nhắn trong phiên thảo luận. Học sinh gửi thì sessionAgent = null.
 */
@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @UuidGenerator
    @Column(name = "message_id", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private DiscussionSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_agent_id")
    private SessionAgent sessionAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private Message replyTo;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 10)
    private SenderType senderType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** Đánh dấu câu phản biện AI cố ý nói sai logic */
    @Column(name = "has_intended_error", nullable = false)
    @Builder.Default
    private Boolean hasIntendedError = false;

    /** Giải thích chỗ sai, chỉ trả cho học sinh sau khi bắt được lỗi */
    @Column(name = "error_note", columnDefinition = "TEXT")
    private String errorNote;

    /** Các nút trả lời nhanh để học sinh bấm thay vì gõ */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "quick_replies", columnDefinition = "jsonb")
    private List<QuickReply> quickReplies;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Gán thời gian tạo nếu service chưa gán (service gán tăng dần để giữ đúng thứ tự hội thoại) */
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

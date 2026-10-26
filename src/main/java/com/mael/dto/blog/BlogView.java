package com.mael.dto.blog;

import java.time.LocalDateTime;

public record BlogView(
        String id,
        String title,
        String summary,
        String content,
        String coverUrl,
        String theme,
        String tag,
        boolean published,
        boolean pinned,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

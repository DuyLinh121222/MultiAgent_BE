package com.mael.dto.discussion;

import com.mael.entity.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SessionView(
        String id,
        String title,
        SessionStatus status,
        String summary,
        String lessonId,
        String lessonTitle,
        String lessonContent,
        String topicName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        List<AgentView> agents,
        List<MessageView> messages
) {
}

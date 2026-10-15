package com.mael.controller;

import com.mael.dto.discussion.*;
import com.mael.service.DiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Phòng học nhóm AI: học sinh thảo luận cùng Thầy AI và 2 bạn AI.
 */
@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionView start(@Valid @RequestBody StartSessionRequest req) {
        return discussionService.start(req);
    }

    @GetMapping
    public List<SessionSummaryView> mySessions() {
        return discussionService.mySessions();
    }

    @GetMapping("/{sessionId}")
    public SessionView get(@PathVariable String sessionId) {
        return discussionService.get(sessionId);
    }

    /** Gửi tin nhắn, nhận lại tin của học sinh + các câu trả lời của AI */
    @PostMapping("/{sessionId}/messages")
    public List<MessageView> send(@PathVariable String sessionId, @Valid @RequestBody SendMessageRequest req) {
        return discussionService.sendMessage(sessionId, req);
    }

    /** Học sinh đánh dấu một câu của AI là sai */
    @PostMapping("/{sessionId}/messages/{messageId}/flag-error")
    public FlagErrorResponse flagError(@PathVariable String sessionId, @PathVariable String messageId) {
        return discussionService.flagError(sessionId, messageId);
    }

    @PostMapping("/{sessionId}/end")
    public SessionView end(@PathVariable String sessionId) {
        return discussionService.end(sessionId);
    }
}

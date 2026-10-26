package com.mael.service;

import com.mael.dto.discussion.*;
import com.mael.entity.*;
import com.mael.entity.enums.SenderType;
import com.mael.entity.enums.SessionStatus;
import com.mael.exception.ApiException;
import com.mael.repository.AiAgentRepository;
import com.mael.repository.DiscussionSessionRepository;
import com.mael.repository.MessageRepository;
import com.mael.security.CurrentUser;
import com.mael.service.ai.AgentReply;
import com.mael.service.ai.AiOrchestrator;
import com.mael.service.ai.DiscussionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Phòng học nhóm: 1 học sinh + Thầy AI + 2 bạn AI thảo luận về một bài giảng.
 */
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionSessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final AiAgentRepository agentRepository;
    private final LearningService learningService;
    private final SubscriptionService subscriptionService;
    private final AchievementService achievementService;
    private final AiOrchestrator aiOrchestrator;
    private final CurrentUser currentUser;
    private final com.mael.repository.QuestionRepository questionRepository;

    @Transactional
    public SessionView start(StartSessionRequest req) {
        Student student = currentUser.student();
        subscriptionService.checkAiSessionQuota(student);
        Lesson lesson = learningService.getPublishedLesson(req.lessonId());

        DiscussionSession session = DiscussionSession.builder()
                .student(student)
                .lesson(lesson)
                .title("Thảo luận: " + lesson.getTitle())
                .build();
        // Thêm tất cả tác tử đang hoạt động vào phiên (1 gia sư + các bạn phản biện)
        agentRepository.findAll().stream()
                .filter(AiAgent::getActive)
                .forEach(agent -> session.getAgents().add(SessionAgent.builder()
                        .session(session).agent(agent).roleInSession(agent.getAgentType()).build()));
        sessionRepository.save(session);

        saveAgentReplies(session, aiOrchestrator.openingMessages(buildContext(session, List.of())), null);
        return toView(session);
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryView> mySessions() {
        return sessionRepository.findByStudentId(currentUser.student().getId()).stream()
                .map(s -> new SessionSummaryView(s.getId(), s.getTitle(), s.getLesson().getId(),
                        s.getLesson().getTitle(), s.getStatus(), s.getStartedAt(), s.getEndedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionView get(String sessionId) {
        return toView(getOwnSession(sessionId));
    }

    /**
     * Học sinh gửi tin nhắn, các AI trả lời. Trả về các tin nhắn mới (của học sinh + AI).
     */
    @Transactional
    public List<MessageView> sendMessage(String sessionId, SendMessageRequest req) {
        DiscussionSession session = getOwnSession(sessionId);
        if (session.getStatus() == SessionStatus.ENDED) {
            throw ApiException.badRequest("Phiên thảo luận đã kết thúc");
        }
        subscriptionService.requireActive(session.getStudent());

        Message replyTo = req.replyToId() == null ? null : messageRepository.findById(req.replyToId())
                .filter(m -> m.getSession().getId().equals(sessionId))
                .orElse(null);
        // Học sinh bấm nút trả lời nhanh của tin nhắn AI gần nhất?
        List<Message> before = messageRepository.findBySessionId(sessionId);
        String content = req.content().trim();
        QuickReply selected = latestQuickReplies(before).stream()
                .filter(q -> q.text().equalsIgnoreCase(content))
                .findFirst().orElse(null);

        Message studentMsg = messageRepository.save(Message.builder()
                .session(session)
                .senderType(SenderType.STUDENT)
                .content(content)
                .replyTo(replyTo)
                .createdAt(LocalDateTime.now())
                .build());

        if (selected != null && Boolean.TRUE.equals(selected.catchError()) && Boolean.TRUE.equals(selected.correct())) {
            achievementService.award(session.getStudent(), AchievementService.BUG_HUNTER);
        }

        List<Message> history = messageRepository.findBySessionId(sessionId);
        List<AgentReply> replies = aiOrchestrator.reply(buildContext(session, history), content, selected);

        List<MessageView> result = new ArrayList<>();
        result.add(DtoMapper.toMessageView(studentMsg));
        saveAgentReplies(session, replies, studentMsg).forEach(m -> result.add(DtoMapper.toMessageView(m)));
        return result;
    }

    /**
     * Học sinh bấm "Bạn ấy nói sai!" trên tin nhắn của AI.
     */
    @Transactional
    public FlagErrorResponse flagError(String sessionId, String messageId) {
        DiscussionSession session = getOwnSession(sessionId);
        Message m = messageRepository.findById(messageId)
                .filter(x -> x.getSession().getId().equals(sessionId) && x.getSenderType() == SenderType.AGENT)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tin nhắn"));
        if (Boolean.TRUE.equals(m.getHasIntendedError())) {
            boolean unlocked = achievementService.award(session.getStudent(), AchievementService.BUG_HUNTER);
            return new FlagErrorResponse(true,
                    "Chính xác! Bạn đã phát hiện lập luận sai.", m.getErrorNote(), unlocked);
        }
        return new FlagErrorResponse(false,
                "Câu này thực ra đúng đó. Bạn đọc lại thật kỹ hoặc hỏi Thầy AI nhé!", null, false);
    }

    @Transactional
    public SessionView end(String sessionId) {
        DiscussionSession session = getOwnSession(sessionId);
        if (session.getStatus() == SessionStatus.ACTIVE) {
            List<Message> history = messageRepository.findBySessionId(sessionId);
            session.setSummary(aiOrchestrator.summarize(buildContext(session, history)));
            session.setStatus(SessionStatus.ENDED);
            session.setEndedAt(LocalDateTime.now());
        }
        return toView(session);
    }

    // ------------------------------------------------------------------

    private DiscussionSession getOwnSession(String sessionId) {
        String studentId = currentUser.student().getId();
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getStudent().getId().equals(studentId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiên thảo luận"));
    }

    private List<Message> saveAgentReplies(DiscussionSession session, List<AgentReply> replies, Message replyTo) {
        Map<String, SessionAgent> byAgentId = session.getAgents().stream()
                .collect(Collectors.toMap(sa -> sa.getAgent().getId(), Function.identity()));
        List<Message> saved = new ArrayList<>();
        LocalDateTime time = LocalDateTime.now();
        for (AgentReply r : replies) {
            time = time.plusNanos(1_000_000); // +1ms mỗi tin để giữ đúng thứ tự
            SessionAgent sa = byAgentId.get(r.agentId());
            if (sa == null) continue;
            saved.add(messageRepository.save(Message.builder()
                    .session(session)
                    .sessionAgent(sa)
                    .senderType(SenderType.AGENT)
                    .content(r.content())
                    .hasIntendedError(r.hasIntendedError())
                    .errorNote(r.errorNote())
                    .quickReplies(r.quickReplies() == null || r.quickReplies().isEmpty() ? null : r.quickReplies())
                    .replyTo(replyTo)
                    .createdAt(time)
                    .build()));
        }
        return saved;
    }

    private DiscussionContext buildContext(DiscussionSession session, List<Message> history) {
        Lesson lesson = session.getLesson();
        List<DiscussionContext.AgentInfo> agents = session.getAgents().stream()
                .map(sa -> new DiscussionContext.AgentInfo(sa.getAgent().getId(), sa.getAgent().getName(),
                        sa.getRoleInSession(), sa.getAgent().getPersonaPrompt(), sa.getAgent().getModelName()))
                .toList();
        String studentName = session.getStudent().getFullName();
        List<DiscussionContext.Turn> turns = history.stream()
                .map(m -> m.getSenderType() == SenderType.STUDENT
                        ? new DiscussionContext.Turn(studentName, true, m.getContent(), false)
                        : new DiscussionContext.Turn(m.getSessionAgent().getAgent().getName(), false, m.getContent(),
                        Boolean.TRUE.equals(m.getHasIntendedError())))
                .toList();
        int maxRound = history.stream()
                .filter(m -> m.getQuickReplies() != null)
                .flatMap(m -> m.getQuickReplies().stream())
                .map(QuickReply::round)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compare)
                .orElse(-1);
        return new DiscussionContext(session.getId(), studentName, lesson.getTitle(), lesson.getContent(),
                lesson.getTopic().getName(),
                lesson.getMisconceptions() == null ? List.of() : lesson.getMisconceptions(),
                agents, turns, latestQuickReplies(history), maxRound, practice(lesson));
    }

    /** Câu trắc nghiệm đã duyệt của bài, dùng cho lượt "áp dụng công thức" trong phòng thảo luận */
    private List<DiscussionContext.PracticeQuestion> practice(Lesson lesson) {
        return questionRepository.findByLessonIdAndReviewStatusOrderByCreatedAtAsc(lesson.getId(),
                        com.mael.entity.enums.ReviewStatus.PUBLISHED).stream()
                .filter(q -> q.getQuestionType() == com.mael.entity.enums.QuestionType.SINGLE_CHOICE
                        && q.getOptions() != null && q.getOptions().size() >= 2)
                .map(q -> {
                    List<String> opts = q.getOptions().stream().map(o -> o.replaceFirst("^\\s*[A-Fa-f][.)]\\s*", "").trim()).toList();
                    String letter = q.getCorrectAnswer().trim().substring(0, 1).toUpperCase();
                    int idx = Math.max(0, Math.min(opts.size() - 1, letter.charAt(0) - 'A'));
                    return new DiscussionContext.PracticeQuestion(q.getId(), q.getContent(), opts, idx, q.getExplanation());
                })
                .toList();
    }

    /** Các nút trả lời nhanh của tin nhắn AI gần nhất (nếu tin nhắn cuối cùng của AI có nút) */
    private static List<QuickReply> latestQuickReplies(List<Message> history) {
        int i = history.size() - 1;
        // Bỏ qua tin nhắn học sinh vừa gửi ở cuối
        while (i >= 0 && history.get(i).getSenderType() == SenderType.STUDENT) i--;
        for (; i >= 0; i--) {
            Message m = history.get(i);
            if (m.getSenderType() == SenderType.STUDENT) return List.of();
            if (m.getQuickReplies() != null && !m.getQuickReplies().isEmpty()) return m.getQuickReplies();
        }
        return List.of();
    }

    private SessionView toView(DiscussionSession s) {
        Lesson lesson = s.getLesson();
        return new SessionView(s.getId(), s.getTitle(), s.getStatus(), s.getSummary(),
                lesson.getId(), lesson.getTitle(), lesson.getContent(), lesson.getTopic().getName(),
                s.getStartedAt(), s.getEndedAt(),
                s.getAgents().stream().map(DtoMapper::toAgentView).toList(),
                messageRepository.findBySessionId(s.getId()).stream().map(DtoMapper::toMessageView).toList());
    }
}

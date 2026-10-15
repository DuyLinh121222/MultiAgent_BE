package com.mael.service;

import com.mael.dto.review.CreateLessonRequest;
import com.mael.dto.review.CreateQuestionRequest;
import com.mael.entity.Lesson;
import com.mael.entity.Question;
import com.mael.entity.Topic;
import com.mael.entity.UserAccount;
import com.mael.entity.enums.ContentSource;
import com.mael.entity.enums.Difficulty;
import com.mael.entity.enums.ReviewStatus;
import com.mael.exception.ApiException;
import com.mael.repository.LessonRepository;
import com.mael.repository.QuestionRepository;
import com.mael.repository.TopicRepository;
import com.mael.repository.UserAccountRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Map;

/**
 * Admin soạn bài giảng / câu hỏi (DRAFT) và gửi giảng viên duyệt (PENDING_REVIEW).
 */
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final TopicRepository topicRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUser currentUser;

    @Transactional
    public Map<String, String> createLesson(CreateLessonRequest req) {
        Topic topic = topicRepository.findById(req.topicId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chủ đề"));
        UserAccount admin = userAccountRepository.getReferenceById(currentUser.userId());
        Lesson lesson = lessonRepository.save(Lesson.builder()
                .topic(topic)
                .title(req.title())
                .content(req.content())
                .videoUrl(req.videoUrl())
                .durationMinutes(req.durationMinutes())
                .orderIndex(lessonRepository.findMaxOrderIndex(topic.getId()) + 1)
                .createdBy(admin)
                .reviewStatus(ReviewStatus.DRAFT)
                .build());
        return Map.of("id", lesson.getId(), "reviewStatus", lesson.getReviewStatus().name());
    }

    @Transactional
    public Map<String, String> createQuestion(CreateQuestionRequest req) {
        Topic topic = topicRepository.findById(req.topicId())
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy chủ đề"));
        Question q = questionRepository.save(Question.builder()
                .topic(topic)
                .content(req.content())
                .questionType(req.questionType())
                .options(req.options())
                .correctAnswer(req.correctAnswer())
                .explanation(req.explanation())
                .difficulty(req.difficulty() != null ? req.difficulty() : Difficulty.MEDIUM)
                .source(ContentSource.MANUAL)
                .createdBy(userAccountRepository.getReferenceById(currentUser.userId()))
                .reviewStatus(ReviewStatus.DRAFT)
                .build());
        return Map.of("id", q.getId(), "reviewStatus", q.getReviewStatus().name());
    }

    @Transactional
    public void submitLessonForReview(String lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài giảng"));
        if (!EnumSet.of(ReviewStatus.DRAFT, ReviewStatus.REJECTED).contains(lesson.getReviewStatus())) {
            throw ApiException.badRequest("Chỉ gửi duyệt được bài ở trạng thái DRAFT hoặc REJECTED");
        }
        lesson.setReviewStatus(ReviewStatus.PENDING_REVIEW);
        // TODO: gửi email báo giảng viên có bài chờ duyệt (SMTP)
    }

    @Transactional
    public void submitQuestionForReview(String questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy câu hỏi"));
        if (!EnumSet.of(ReviewStatus.DRAFT, ReviewStatus.REJECTED).contains(q.getReviewStatus())) {
            throw ApiException.badRequest("Chỉ gửi duyệt được câu hỏi ở trạng thái DRAFT hoặc REJECTED");
        }
        q.setReviewStatus(ReviewStatus.PENDING_REVIEW);
    }
}

package com.mael.service;

import com.mael.dto.review.ReportRequest;
import com.mael.dto.review.ReportView;
import com.mael.entity.ContentReport;
import com.mael.entity.Student;
import com.mael.exception.ApiException;
import com.mael.repository.*;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Học sinh báo nội dung sai (bài giảng, câu hỏi, câu trả lời AI, lời giải ảnh).
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ContentReportRepository reportRepository;
    private final LessonRepository lessonRepository;
    private final QuestionRepository questionRepository;
    private final MessageRepository messageRepository;
    private final SolutionRepository solutionRepository;
    private final CurrentUser currentUser;

    @Transactional
    public ReportView create(ReportRequest req) {
        Student student = currentUser.student();
        ContentReport.ContentReportBuilder b = ContentReport.builder()
                .student(student)
                .reason(req.reason())
                .description(req.description());
        switch (req.targetType()) {
            case LESSON -> b.lesson(lessonRepository.findById(req.targetId())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài giảng")));
            case QUESTION -> b.question(questionRepository.findById(req.targetId())
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy câu hỏi")));
            case MESSAGE -> b.message(messageRepository.findById(req.targetId())
                    .filter(m -> m.getSession().getStudent().getId().equals(student.getId()))
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy tin nhắn")));
            case SOLUTION -> b.solution(solutionRepository.findById(req.targetId())
                    .filter(s -> s.getPhotoProblem().getStudent().getId().equals(student.getId()))
                    .orElseThrow(() -> ApiException.notFound("Không tìm thấy lời giải")));
        }
        return ReviewService.toReportView(reportRepository.save(b.build()));
    }
}

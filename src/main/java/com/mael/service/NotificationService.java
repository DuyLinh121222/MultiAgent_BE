package com.mael.service;

import com.mael.dto.common.NotificationView;
import com.mael.entity.Notification;
import com.mael.entity.Payment;
import com.mael.entity.Student;
import com.mael.entity.enums.NotificationType;
import com.mael.exception.ApiException;
import com.mael.repository.NotificationRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUser currentUser;

    @Transactional
    public void notify(Student student, NotificationType type, String title, String content, Payment payment) {
        notificationRepository.save(Notification.builder()
                .student(student)
                .type(type)
                .title(title)
                .content(content)
                .payment(payment)
                .build());
    }

    @Transactional(readOnly = true)
    public List<NotificationView> myNotifications() {
        return notificationRepository.findByStudentIdOrderByCreatedAtDesc(currentUser.student().getId())
                .stream().map(DtoMapper::toNotificationView).toList();
    }

    @Transactional
    public void markRead(String notificationId) {
        String studentId = currentUser.student().getId();
        Notification n = notificationRepository.findById(notificationId)
                .filter(x -> x.getStudent().getId().equals(studentId))
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy thông báo"));
        n.setRead(true);
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.findByStudentIdOrderByCreatedAtDesc(currentUser.student().getId())
                .forEach(n -> n.setRead(true));
    }
}

package com.mael.service.admin;

import com.mael.dto.admin.AuditLogView;
import com.mael.dto.admin.PageResponse;
import com.mael.entity.AuditLog;
import com.mael.repository.AuditLogRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ghi và xem nhật ký thao tác quản trị.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUser currentUser;

    /** Ghi trong cùng transaction với thao tác: thao tác lỗi (rollback) thì không ghi nhật ký */
    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String action, String targetType, String targetId, String detail) {
        auditLogRepository.save(AuditLog.builder()
                .actorId(currentUser.userId())
                .actorName(currentUser.displayName())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogView> list(String action, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<AuditLog> p = action == null || action.isBlank()
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pr)
                : auditLogRepository.findByActionOrderByCreatedAtDesc(action, pr);
        return new PageResponse<>(p.getContent().stream().map(AuditService::toView).toList(),
                p.getTotalElements(), p.getNumber(), p.getSize());
    }

    @Transactional(readOnly = true)
    public List<AuditLogView> recent() {
        return auditLogRepository.findTop8ByOrderByCreatedAtDesc().stream().map(AuditService::toView).toList();
    }

    static AuditLogView toView(AuditLog l) {
        return new AuditLogView(l.getId(), l.getActorName(), l.getAction(), l.getTargetType(), l.getTargetId(),
                l.getDetail(), l.getCreatedAt());
    }
}

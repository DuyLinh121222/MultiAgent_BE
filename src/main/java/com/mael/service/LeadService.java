package com.mael.service;

import com.mael.dto.lead.LeadRequest;
import com.mael.dto.lead.LeadView;
import com.mael.entity.OrgLead;
import com.mael.entity.enums.LeadStatus;
import com.mael.exception.ApiException;
import com.mael.repository.OrgLeadRepository;
import com.mael.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Trung tâm / trường học đăng ký nhận tư vấn gói tổ chức (không cần đăng nhập).
 */
@Service
@RequiredArgsConstructor
public class LeadService {

    private final OrgLeadRepository leadRepository;
    private final SubscriptionPlanRepository planRepository;

    @Transactional
    public LeadView create(LeadRequest req) {
        OrgLead lead = leadRepository.save(OrgLead.builder()
                .orgType(req.orgType())
                .orgName(req.orgName().trim())
                .contactName(req.contactName().trim())
                .phone(req.phone().replace(" ", ""))
                .email(req.email() == null || req.email().isBlank() ? null : req.email().trim())
                .studentCount(req.studentCount())
                .plan(req.planId() == null ? null : planRepository.findById(req.planId()).orElse(null))
                .note(req.note())
                .build());
        return toView(lead);
    }

    @Transactional(readOnly = true)
    public List<LeadView> all() {
        return leadRepository.findAllByOrderByCreatedAtDesc().stream().map(LeadService::toView).toList();
    }

    @Transactional
    public LeadView updateStatus(String id, LeadStatus status) {
        OrgLead lead = leadRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu tư vấn"));
        lead.setStatus(status);
        return toView(lead);
    }

    private static LeadView toView(OrgLead l) {
        return new LeadView(l.getId(), l.getOrgType(), l.getOrgName(), l.getContactName(), l.getPhone(), l.getEmail(),
                l.getStudentCount(), l.getPlan() != null ? l.getPlan().getName() : null, l.getNote(), l.getStatus(),
                l.getCreatedAt());
    }
}

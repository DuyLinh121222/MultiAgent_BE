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
    private final com.mael.service.security.CaptchaService captchaService;

    @org.springframework.beans.factory.annotation.Value("${app.security.lead-max-per-phone-per-day:2}")
    private int maxPerPhone;
    @org.springframework.beans.factory.annotation.Value("${app.security.lead-max-per-ip-per-day:5}")
    private int maxPerIp;

    /**
     * Chống spam: ô bẫy bot, captcha hình, giới hạn theo số điện thoại và IP trong 24 giờ, chặn link trong ghi chú.
     */
    @Transactional
    public LeadView create(LeadRequest req, String ip) {
        if (req.website() != null && !req.website().isBlank()) {
            // Bot điền ô ẩn: trả lỗi chung chung, không lưu
            throw ApiException.badRequest("Không gửi được yêu cầu, vui lòng thử lại");
        }
        captchaService.verify(req.captchaToken(), req.captchaAnswer());
        String phone = normalizePhone(req.phone());
        java.time.LocalDateTime dayAgo = java.time.LocalDateTime.now().minusHours(24);
        if (leadRepository.countByPhoneAndCreatedAtAfter(phone, dayAgo) >= maxPerPhone) {
            throw ApiException.conflict("Số điện thoại này đã gửi yêu cầu hôm nay. MAEL sẽ sớm liên hệ, bạn không cần gửi lại nhé!");
        }
        if (leadRepository.countByIpAddressAndCreatedAtAfter(ip, dayAgo) >= maxPerIp) {
            throw new ApiException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                    "Bạn đã gửi quá nhiều yêu cầu trong hôm nay, vui lòng gọi hotline 0342 764 001");
        }
        String note = req.note() == null ? null : req.note().trim();
        if (note != null && note.matches("(?is).*(https?://|www\\.|<a\\s|\\[url).*")) {
            throw ApiException.badRequest("Ghi chú không được chứa đường link");
        }
        OrgLead lead = leadRepository.save(OrgLead.builder()
                .orgType(req.orgType())
                .orgName(req.orgName().trim())
                .contactName(req.contactName().trim())
                .phone(phone)
                .ipAddress(ip)
                .email(req.email() == null || req.email().isBlank() ? null : req.email().trim())
                .studentCount(req.studentCount())
                .plan(req.planId() == null ? null : planRepository.findById(req.planId()).orElse(null))
                .note(note)
                .build());
        return toView(lead);
    }

    /** "+84 342 764 001" -> "0342764001" */
    static String normalizePhone(String raw) {
        String p = raw.replaceAll("[^0-9+]", "");
        if (p.startsWith("+84")) p = "0" + p.substring(3);
        if (p.length() != 10) throw ApiException.badRequest("Số điện thoại di động gồm 10 số");
        return p;
    }

    @Transactional(readOnly = true)
    public List<LeadView> all() {
        return leadRepository.findAllByOrderByCreatedAtDesc().stream().map(LeadService::toView).toList();
    }

    @Transactional
    public LeadView updateStatus(String id, LeadStatus status, String adminNote) {
        OrgLead lead = leadRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy yêu cầu tư vấn"));
        if (status != null) lead.setStatus(status);
        if (adminNote != null) lead.setAdminNote(adminNote.isBlank() ? null : adminNote.trim());
        return toView(lead);
    }

    private static LeadView toView(OrgLead l) {
        return new LeadView(l.getId(), l.getOrgType(), l.getOrgName(), l.getContactName(), l.getPhone(), l.getEmail(),
                l.getStudentCount(), l.getPlan() != null ? l.getPlan().getName() : null, l.getNote(), l.getStatus(), l.getAdminNote(),
                l.getCreatedAt());
    }
}

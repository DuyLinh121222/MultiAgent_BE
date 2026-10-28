package com.mael.dto.lead;

import com.mael.entity.enums.LeadStatus;
import com.mael.entity.enums.OrgType;

import java.time.LocalDateTime;

public record LeadView(
        String id,
        OrgType orgType,
        String orgName,
        String contactName,
        String phone,
        String email,
        Integer studentCount,
        String planName,
        String note,
        LeadStatus status,
        String adminNote,
        LocalDateTime createdAt
) {
}

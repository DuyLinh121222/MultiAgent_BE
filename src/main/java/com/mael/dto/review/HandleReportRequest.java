package com.mael.dto.review;

import com.mael.entity.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record HandleReportRequest(
        @NotNull ReportStatus status,
        @Size(max = 2000) String resolutionNote
) {
}

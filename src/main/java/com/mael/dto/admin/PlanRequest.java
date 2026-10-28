package com.mael.dto.admin;

import com.mael.entity.enums.PlanAudience;
import com.mael.entity.enums.PriceUnit;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "Nhập tên gói") @Size(max = 100) String name,
        @Size(max = 1000) String description,
        @NotNull(message = "Nhập giá") @DecimalMin(value = "0", message = "Giá không âm") BigDecimal price,
        @NotNull(message = "Nhập số ngày") @Min(value = 1, message = "Thời hạn ít nhất 1 ngày") @Max(3650) Integer durationDays,
        @Min(1) Integer maxAiSessions,
        @NotNull PlanAudience audience,
        @NotNull PriceUnit priceUnit,
        @Min(1) Integer minSeats,
        @Size(max = 2000) String features,
        Boolean featured,
        Integer sortOrder,
        Boolean active
) {
}

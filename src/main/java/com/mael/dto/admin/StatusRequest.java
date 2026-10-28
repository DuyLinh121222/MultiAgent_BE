package com.mael.dto.admin;

import com.mael.entity.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusRequest(
        @NotNull AccountStatus status,
        @Size(max = 300) String reason
) {
}

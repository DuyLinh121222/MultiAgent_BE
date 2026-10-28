package com.mael.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Admin nạp (số dương) hoặc trừ (số âm) xu cho học sinh */
public record CoinAdjustRequest(
        @NotNull(message = "Nhập số xu") @Min(value = -100000, message = "Tối đa trừ 100.000 xu") @Max(value = 100000, message = "Tối đa nạp 100.000 xu") Integer amount,
        @NotBlank(message = "Ghi lý do nạp / trừ xu") @Size(max = 300) String reason
) {
}

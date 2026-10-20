package com.mael.dto.game;

import com.mael.entity.enums.LearningGoal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Màn chào mừng 3 bước: chọn lớp, mục tiêu, thú đồng hành khởi đầu.
 */
public record OnboardingRequest(
        @NotNull(message = "Vui lòng chọn lớp") @Min(6) @Max(9) Integer gradeLevel,
        @NotNull(message = "Vui lòng chọn mục tiêu") LearningGoal goal,
        @NotBlank(message = "Vui lòng chọn thú đồng hành") String petCode
) {
}

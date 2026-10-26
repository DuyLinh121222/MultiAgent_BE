package com.mael.dto.profile;

import com.mael.entity.enums.LearningGoal;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 150, message = "Họ tên tối đa 150 kí tự") String fullName,
        @Pattern(regexp = "^$|^0[0-9]{9}$", message = "Số điện thoại gồm 10 số, bắt đầu bằng 0") String phone,
        @NotNull(message = "Vui lòng chọn lớp") @Min(value = 6, message = "Lớp từ 6 đến 9") @Max(value = 9, message = "Lớp từ 6 đến 9") Integer gradeLevel,
        @Past(message = "Ngày sinh không hợp lệ") LocalDate dateOfBirth,
        LearningGoal learningGoal
) {
}

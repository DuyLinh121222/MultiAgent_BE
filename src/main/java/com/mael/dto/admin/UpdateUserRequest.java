package com.mael.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Admin sửa thông tin hồ sơ. Trường null = giữ nguyên */
public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        @Size(max = 20) String phone,
        @Min(6) @Max(9) Integer gradeLevel,
        @Size(max = 150) String specialization,
        @Size(max = 100) String academicDegree,
        @Size(max = 255) String workplace
) {
}

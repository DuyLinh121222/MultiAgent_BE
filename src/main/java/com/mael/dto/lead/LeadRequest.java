package com.mael.dto.lead;

import com.mael.entity.enums.OrgType;
import jakarta.validation.constraints.*;

public record LeadRequest(
        @NotNull(message = "Vui lòng chọn loại tổ chức") OrgType orgType,
        @NotBlank(message = "Vui lòng nhập tên trung tâm / trường") @Size(max = 200) String orgName,
        @NotBlank(message = "Vui lòng nhập người liên hệ") @Size(max = 150) String contactName,
        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(regexp = "^[0-9+ ]{9,15}$", message = "Số điện thoại không hợp lệ") String phone,
        @Email(message = "Email không hợp lệ") @Size(max = 255) String email,
        @Min(value = 1, message = "Số học sinh phải lớn hơn 0") Integer studentCount,
        String planId,
        @Size(max = 2000) String note
) {
}

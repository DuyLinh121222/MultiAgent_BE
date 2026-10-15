package com.mael.dto.learning;

import com.mael.entity.enums.FileType;

public record MaterialView(
        String id,
        String fileName,
        String fileUrl,
        FileType fileType,
        Long fileSize
) {
}

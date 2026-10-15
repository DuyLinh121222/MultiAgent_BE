package com.mael.dto.photo;

import java.time.LocalDateTime;
import java.util.List;

public record SolutionView(
        String id,
        String finalAnswer,
        List<String> steps,
        String generatedBy,
        LocalDateTime createdAt
) {
}

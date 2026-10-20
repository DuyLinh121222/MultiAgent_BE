package com.mael.dto.game;

import java.util.List;

/** Câu thử thách (không kèm đáp án) */
public record StageQuestion(
        String id,
        String content,
        List<String> options
) {
}

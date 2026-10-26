package com.mael.dto.document;

import java.util.List;

/** Chương / chủ đề để giảng viên chọn nơi gắn tài liệu */
public record TopicOption(String id, String name, int gradeLevel, List<LessonOption> lessons) {

    public record LessonOption(String id, String title, String reviewStatus) {
    }
}

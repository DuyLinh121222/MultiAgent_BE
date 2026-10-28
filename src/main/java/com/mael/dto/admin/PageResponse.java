package com.mael.dto.admin;

import java.util.List;

/** Kết quả có phân trang: items + tổng số bản ghi */
public record PageResponse<T>(List<T> items, long total, int page, int size) {
}

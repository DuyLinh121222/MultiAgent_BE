package com.mael.service.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Giới hạn số lần gọi theo khoá (IP, email...) trong một khoảng thời gian, lưu trong bộ nhớ.
 * Đủ dùng cho 1 máy chủ; khi chạy nhiều máy chủ thì chuyển sang Redis.
 */
@Component
public class RateLimiter {

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Ghi nhận 1 lần gọi. Trả về false nếu đã vượt quá max lần trong window */
    public boolean tryAcquire(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        long from = now - window.toMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < from) q.pollFirst();
            if (q.size() >= max) return false;
            q.addLast(now);
        }
        if (hits.size() > 20_000) cleanup(from);
        return true;
    }

    /** Đếm số lần trong window mà không ghi thêm */
    public int count(String key, Duration window) {
        Deque<Long> q = hits.get(key);
        if (q == null) return 0;
        long from = System.currentTimeMillis() - window.toMillis();
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() < from) q.pollFirst();
            return q.size();
        }
    }

    public void reset(String key) {
        hits.remove(key);
    }

    private void cleanup(long from) {
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || e.getValue().peekLast() < from;
            }
        });
    }
}

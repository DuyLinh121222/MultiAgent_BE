package com.mael.controller;

import com.mael.dto.blog.BlogRequest;
import com.mael.dto.blog.BlogView;
import com.mael.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Bài viết trang chủ: khách xem (/api/blog), giảng viên / admin quản lý (/api/manage/blog).
 * /api/public/site: thông tin liên hệ hiện ở trang chủ.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @Value("${app.site.phone:0342764001}")
    private String phone;
    @Value("${app.site.email:tduyminh0112@gmail.com}")
    private String email;

    @GetMapping("/public/site")
    public Map<String, String> site() {
        return Map.of("phone", phone, "email", email, "name", "MAEL.EDU");
    }

    @GetMapping("/blog")
    public List<BlogView> list() {
        return blogService.published();
    }

    @GetMapping("/blog/{id}")
    public BlogView get(@PathVariable String id) {
        return blogService.publishedOne(id);
    }

    @GetMapping("/manage/blog")
    public List<BlogView> all() {
        return blogService.all();
    }

    @PostMapping("/manage/blog")
    @ResponseStatus(HttpStatus.CREATED)
    public BlogView create(@Valid @RequestBody BlogRequest req) {
        return blogService.create(req);
    }

    @PutMapping("/manage/blog/{id}")
    public BlogView update(@PathVariable String id, @Valid @RequestBody BlogRequest req) {
        return blogService.update(id, req);
    }

    @DeleteMapping("/manage/blog/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        blogService.delete(id);
    }
}

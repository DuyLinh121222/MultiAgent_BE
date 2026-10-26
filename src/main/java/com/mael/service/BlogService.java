package com.mael.service;

import com.mael.dto.blog.BlogRequest;
import com.mael.dto.blog.BlogView;
import com.mael.entity.BlogPost;
import com.mael.entity.UserAccount;
import com.mael.entity.enums.Role;
import com.mael.exception.ApiException;
import com.mael.repository.BlogPostRepository;
import com.mael.repository.LecturerRepository;
import com.mael.repository.UserAccountRepository;
import com.mael.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bài viết hiện ở banner trang chủ cho khách. Giảng viên / admin đăng, sửa, ẩn, ghim.
 */
@Service
@RequiredArgsConstructor
public class BlogService {

    private final BlogPostRepository blogRepository;
    private final UserAccountRepository userAccountRepository;
    private final LecturerRepository lecturerRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<BlogView> published() {
        return blogRepository.findByPublishedTrueOrderByPinnedDescCreatedAtDesc().stream().map(BlogService::toView).toList();
    }

    @Transactional(readOnly = true)
    public BlogView publishedOne(String id) {
        return blogRepository.findById(id).filter(BlogPost::getPublished).map(BlogService::toView)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
    }

    @Transactional(readOnly = true)
    public List<BlogView> all() {
        return blogRepository.findAllByOrderByPinnedDescCreatedAtDesc().stream().map(BlogService::toView).toList();
    }

    @Transactional
    public BlogView create(BlogRequest req) {
        UserAccount me = userAccountRepository.findById(currentUser.userId()).orElseThrow();
        BlogPost p = BlogPost.builder().author(me).authorName(authorName(me)).build();
        apply(p, req);
        return toView(blogRepository.save(p));
    }

    @Transactional
    public BlogView update(String id, BlogRequest req) {
        BlogPost p = blogRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy bài viết"));
        apply(p, req);
        return toView(p);
    }

    @Transactional
    public void delete(String id) {
        if (!blogRepository.existsById(id)) throw ApiException.notFound("Không tìm thấy bài viết");
        blogRepository.deleteById(id);
    }

    private void apply(BlogPost p, BlogRequest req) {
        p.setTitle(req.title().trim());
        p.setSummary(blank(req.summary()));
        p.setContent(req.content().trim());
        p.setCoverUrl(blank(req.coverUrl()));
        p.setTheme(req.theme() == null || req.theme().isBlank() ? "BLUE" : req.theme());
        p.setTag(blank(req.tag()));
        p.setPublished(req.published() == null || req.published());
        p.setPinned(Boolean.TRUE.equals(req.pinned()));
    }

    private String authorName(UserAccount u) {
        if (u.getRole() == Role.LECTURER) {
            return lecturerRepository.findByUserId(u.getId()).map(l -> l.getFullName()).orElse("Giảng viên MAEL");
        }
        return "Ban biên tập MAEL";
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static BlogView toView(BlogPost p) {
        return new BlogView(p.getId(), p.getTitle(), p.getSummary(), p.getContent(), p.getCoverUrl(), p.getTheme(), p.getTag(),
                p.getPublished(), p.getPinned(), p.getAuthorName(), p.getCreatedAt(), p.getUpdatedAt());
    }
}

package com.mael.repository;

import com.mael.entity.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogPostRepository extends JpaRepository<BlogPost, String> {

    List<BlogPost> findByPublishedTrueOrderByPinnedDescCreatedAtDesc();

    List<BlogPost> findAllByOrderByPinnedDescCreatedAtDesc();
}

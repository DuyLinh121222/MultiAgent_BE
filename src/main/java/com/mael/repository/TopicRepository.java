package com.mael.repository;

import com.mael.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, String> {

    List<Topic> findByLearningPathIdOrderByOrderIndexAsc(String pathId);
}

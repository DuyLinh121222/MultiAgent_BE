package com.mael.repository;

import com.mael.entity.Lesson;
import com.mael.entity.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, String> {

    List<Lesson> findByTopicIdAndReviewStatusOrderByOrderIndexAsc(String topicId, ReviewStatus status);

    @Query("select l from Lesson l join fetch l.topic t where t.learningPath.id = :pathId and l.reviewStatus = :status order by t.orderIndex, l.orderIndex")
    List<Lesson> findByPathAndStatus(String pathId, ReviewStatus status);

    @Query("select l from Lesson l join fetch l.topic t join fetch t.learningPath where l.reviewStatus in :statuses order by l.updatedAt desc")
    List<Lesson> findByReviewStatusIn(Collection<ReviewStatus> statuses);

    @Query("select coalesce(max(l.orderIndex), 0) from Lesson l where l.topic.id = :topicId")
    int findMaxOrderIndex(String topicId);
}

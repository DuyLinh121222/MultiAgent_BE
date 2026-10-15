package com.mael.repository;

import com.mael.entity.Question;
import com.mael.entity.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, String> {

    List<Question> findByTopicIdAndReviewStatus(String topicId, ReviewStatus status);

    @Query("select q from Question q join fetch q.topic where q.reviewStatus in :statuses order by q.createdAt desc")
    List<Question> findByReviewStatusIn(Collection<ReviewStatus> statuses);
}

package org.mj.trip.destination.repository;

import org.mj.trip.destination.domain.RecommendationReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationReasonRepository extends JpaRepository<RecommendationReason, Long> {

    List<RecommendationReason> findByRecommendationId(Long recommendationId);

    void deleteByRecommendationId(Long recommendationId);

    void deleteByRecommendationIdIn(List<Long> recommendationIds);
}

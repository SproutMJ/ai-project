package org.mj.trip.destination.repository;

import org.mj.trip.destination.domain.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByRecommendationRequestIdOrderByRankOrderAsc(Long recommendationRequestId);

    void deleteByRecommendationRequestId(Long recommendationRequestId);
}

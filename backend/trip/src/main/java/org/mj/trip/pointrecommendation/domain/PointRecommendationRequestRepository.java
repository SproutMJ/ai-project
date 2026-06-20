package org.mj.trip.pointrecommendation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointRecommendationRequestRepository extends JpaRepository<PointRecommendationRequest, Long> {

    Page<PointRecommendationRequest> findByUserId(Long userId, Pageable pageable);
}

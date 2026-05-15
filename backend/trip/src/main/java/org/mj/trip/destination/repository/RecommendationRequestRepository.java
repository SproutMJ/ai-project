package org.mj.trip.destination.repository;

import org.mj.trip.destination.domain.RecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {

    List<RecommendationRequest> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}

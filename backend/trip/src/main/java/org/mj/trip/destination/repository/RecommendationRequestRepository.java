package org.mj.trip.destination.repository;

import org.mj.trip.destination.domain.RecommendationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long>, JpaSpecificationExecutor<RecommendationRequest> {

    List<RecommendationRequest> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Page<RecommendationRequest> findAll(Pageable pageable);
}

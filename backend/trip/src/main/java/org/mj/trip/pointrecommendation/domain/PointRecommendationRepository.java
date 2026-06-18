
package org.mj.trip.pointrecommendation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointRecommendationRepository extends JpaRepository<PointRecommendation, Long> {

    List<PointRecommendation> findByRequestOrderByRecommendationScoreDesc(PointRecommendationRequest request);

    Page<PointRecommendation> findByRequestOrderByRecommendationScoreDesc(PointRecommendationRequest request, Pageable pageable);

    @Modifying
    @Query("DELETE FROM PointRecommendation p WHERE p.request.id = :requestId")
    void deleteByRequestId(@Param("requestId") Long requestId);
}

package org.mj.trip.plan.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRecommendationRepository extends JpaRepository<RouteRecommendation, Long> {

    List<RouteRecommendation> findByUserId(Long userId);

    Optional<RouteRecommendation> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT r FROM RouteRecommendation r " +
            "LEFT JOIN FETCH r.daySchedules d " +
            "WHERE r.id = :id")
    Optional<RouteRecommendation> findWithItemsById(@Param("id") Long id);
}

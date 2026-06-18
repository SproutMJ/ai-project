package org.mj.trip.plan.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteDayScheduleRepository extends JpaRepository<RouteDaySchedule, Long> {

    List<RouteDaySchedule> findByRouteRecommendationIdOrderByDayNumberAsc(Long routeRecommendationId);
}

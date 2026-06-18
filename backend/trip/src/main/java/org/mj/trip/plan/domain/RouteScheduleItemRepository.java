package org.mj.trip.plan.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteScheduleItemRepository extends JpaRepository<RouteScheduleItem, Long> {
    List<RouteScheduleItem> findByDayScheduleId(Long dayScheduleId);
}

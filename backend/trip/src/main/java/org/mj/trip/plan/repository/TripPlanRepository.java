package org.mj.trip.plan.repository;

import org.mj.trip.plan.domain.TripPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {

    // 단일 상태 필터
    Page<TripPlan> findByStatus(String status, Pageable pageable);

    // 다중 상태 필터
    Page<TripPlan> findByStatusIn(List<String> statuses, Pageable pageable);

    // 복합 필터 (명세서 기준)
    @Query("SELECT t FROM TripPlan t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:region IS NULL OR t.region = :region) AND " +
           "(:startDateFrom IS NULL OR t.startDate >= :startDateFrom) AND " +
           "(:startDateTo IS NULL OR t.startDate <= :startDateTo) AND " +
           "(:createdFrom IS NULL OR t.createdAt >= :createdFrom) AND " +
           "(:createdTo IS NULL OR t.createdAt <= :createdTo)")
    Page<TripPlan> applyFilters(
            @Param("status") String status,
            @Param("region") String region,
            @Param("startDateFrom") LocalDate startDateFrom,
            @Param("startDateTo") LocalDate startDateTo,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable);

    Optional<TripPlan> findByIdAndDeletedAtIsNull(Long id);
}

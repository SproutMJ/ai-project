package org.mj.trip.plan.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TripPlanDetailResponse(
        Long tripPlanId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String region,
        String description,
        List<TripPlanPointDetailResponse> points,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

package org.mj.trip.plan.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TripPlanListItemResponse(
        Long tripPlanId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        String region,
        String status,
        LocalDateTime createdAt
) {}

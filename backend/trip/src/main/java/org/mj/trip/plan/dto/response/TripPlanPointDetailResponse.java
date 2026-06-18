package org.mj.trip.plan.dto.response;

public record TripPlanPointDetailResponse(
        Long pointId,
        String pointName,
        String address,
        String note,
        Double recommendationScore
) {}

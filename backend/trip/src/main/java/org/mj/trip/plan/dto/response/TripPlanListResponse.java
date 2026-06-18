package org.mj.trip.plan.dto.response;

import java.util.List;

public record TripPlanListResponse(
        List<TripPlanListItemResponse> items,
        int currentPage,
        int pageSize,
        int totalPages,
        long totalItems
) {}

package org.mj.trip.plan.dto;

import java.util.List;

public record TripPlanCreateResponse(
        Long tripPlanId,
        Long tripPlanRequestId,
        String status,
        SummaryResponse summary,
        List<DayResponse> days,
        String createdAt
) {
    public record SummaryResponse(String text, List<String> keyPoints) {}
    public record DayResponse(Integer dayNo, String planDate, List<ItemResponse> items) {}
    public record ItemResponse(String startTime, String endTime, String itemType, String placeName) {}
}

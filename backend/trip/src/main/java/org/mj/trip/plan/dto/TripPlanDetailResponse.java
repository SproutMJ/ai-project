package org.mj.trip.plan.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class TripPlanDetailResponse {
    private Long tripPlanId;
    private String status;
    private Request request;
    private Summary summary;
    private List<Day> days;

    @Getter
    @Builder
    public static class Request {
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal budgetAmount;
    }

    @Getter
    @Builder
    public static class Summary {
        private String text;
    }

    @Getter
    @Builder
    public static class Day {
        private Integer dayNo;
        private LocalDate planDate;
        private List<Item> items;
    }

    @Getter
    @Builder
    public static class Item {
        private Long itemId;
        private String startTime;
        private String endTime;
        private String itemType;
        private String placeName;
    }
}

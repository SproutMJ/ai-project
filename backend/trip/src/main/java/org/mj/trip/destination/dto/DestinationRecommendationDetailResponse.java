package org.mj.trip.destination.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DestinationRecommendationDetailResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DestinationRecommendationDetailData {

    private String tripPurpose;
    private String budgetRange;
    private String region;
    private String season;
    private LocalDateTime createdAt;
    private List<RecommendationItem> recommendations;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationItem {
        private Long recommendationId;
        private Long destinationId;
        private String destinationName;
        private Double score;
        private Integer rankOrder;
        private String reasonSummary;
        private List<ReasonDetail> reasons;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ReasonDetail {
        private String type;
        private String text;
    }
}
}

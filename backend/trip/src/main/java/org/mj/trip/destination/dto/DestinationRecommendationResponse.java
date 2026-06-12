package org.mj.trip.destination.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class DestinationRecommendationResponse {

    private Long recommendationRequestId;
    private List<Recommendation> recommendations;
    private String createdAt;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class RecommendationSummary {
        private Long recommendationRequestId;
        private String summary;
        private LocalDateTime createdAt;

        private String region;
        private String tripPurpose;
        private String budgetRange;
        private String season;
        private Integer companionCount;
        private Integer durationDays;
    }

    @Getter
    @Builder
    public static class Recommendation {
        private Long recommendationId;
        private Long destinationId;
        private String destinationName;
        private Double score;
        private Integer rankOrder;
        private String reasonSummary;
    }

    @Builder
    public DestinationRecommendationResponse(Long recommendationRequestId,
                                             List<Recommendation> recommendations,
                                             LocalDateTime createdAt) {
        this.recommendationRequestId = recommendationRequestId;
        this.recommendations = recommendations;
        this.createdAt = createdAt != null ? createdAt.toString() : null;
    }
}

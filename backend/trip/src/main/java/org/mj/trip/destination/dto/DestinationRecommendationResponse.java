package org.mj.trip.destination.dto;

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
    public static class Recommendation {
        private Long recommendationId;
        private Long destinationId;
        private String destinationName;
        private Double score;
        private Integer rankOrder;
        private String reasonSummary;
        private List<Reason> reasons;
    }

    @Getter
    @Builder
    public static class Reason {
        private String type;
        private String text;
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

package org.mj.trip.pointrecommendation.dto;

import java.util.List;

public record PointRecommendationListResponseDto(
        List<RecommendationSummary> recommendations,
        Meta meta
) {
    public record RecommendationSummary(
            Long id,
            String name,
            String shortComment,
            String type,
            String region,
            Double recommendationScore
    ) {
        public static RecommendationSummary from(org.mj.trip.pointrecommendation.domain.PointRecommendation rec) {
            return new RecommendationSummary(
                    rec.getId(),
                    rec.getName(),
                    rec.getShortComment(),
                    rec.getType(),
                    rec.getRegion(),
                    rec.getRecommendationScore()
            );
        }
    }

    public record Meta(
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static Meta from(org.springframework.data.domain.Page<?> page) {
            return new Meta(
                    page.getNumber() + 1,
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }
}

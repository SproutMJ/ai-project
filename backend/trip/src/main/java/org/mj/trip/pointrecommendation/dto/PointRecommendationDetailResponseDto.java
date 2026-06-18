package org.mj.trip.pointrecommendation.dto;

import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;

import java.time.LocalDateTime;
import java.util.List;

public record PointRecommendationDetailResponseDto(
        RequestInfo requestInfo,
        List<RecommendationInfo> recommendations
) {
    public record RequestInfo(
            Long requestId,
            Long userId,
            String requestText,
            LocalDateTime createdAt
    ) {
        public static RequestInfo from(PointRecommendationRequest request) {
            return new RequestInfo(
                    request.getId(),
                    request.getUserId(),
                    request.getRequestText(),
                    request.getCreatedAt()
            );
        }
    }

    public record RecommendationInfo(
            Long id,
            String name,
            String shortComment,
            String type,
            String region,
            String keyword,
            String theme,
            String budget,
            String requiredTime,
            String howToGo,
            String recommendedPartySize,
            String weather,
            String language,
            String disadvantage,
            String description,
            Double recommendationScore
    ) {
        public static RecommendationInfo from(org.mj.trip.pointrecommendation.domain.PointRecommendation rec) {
            return new RecommendationInfo(
                    rec.getId(),
                    rec.getName(),
                    rec.getShortComment(),
                    rec.getType(),
                    rec.getRegion(),
                    rec.getKeyword(),
                    rec.getTheme(),
                    rec.getBudget(),
                    rec.getRequiredTime(),
                    rec.getHowToGo(),
                    rec.getRecommendedPartySize(),
                    rec.getWeather(),
                    rec.getLanguage(),
                    rec.getDisadvantage(),
                    rec.getDescription(),
                    rec.getRecommendationScore()
            );
        }
    }
}

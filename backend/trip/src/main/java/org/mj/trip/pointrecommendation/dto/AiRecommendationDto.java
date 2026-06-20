package org.mj.trip.pointrecommendation.dto;

public record AiRecommendationDto(
        String name,
        Double recommendationScore,
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
        String description
) {}

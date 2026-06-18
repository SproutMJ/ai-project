package org.mj.trip.pointrecommendation.dto;

public record PointRecommendationListRequestDto(
        int page,
        int size,
        String sort,
        String order
) { }

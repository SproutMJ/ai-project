package org.mj.trip.pointrecommendation.dto;

import jakarta.validation.constraints.NotBlank;

public record PointRecommendationRequestResponseDto (
        Long id,
        @NotBlank
        String requestText
) {}

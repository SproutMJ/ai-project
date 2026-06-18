package org.mj.trip.pointrecommendation.dto;

import jakarta.validation.constraints.NotBlank;

public record PointRecommendationRequestDto (
    @NotBlank
    String requestText
) {}

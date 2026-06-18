package org.mj.trip.plan.dto.request;

import jakarta.validation.constraints.NotNull;

public record PointRecommendationCreateRequest(
        @NotNull(message = "포인트 ID는 필수입니다.")
        Long pointId,

        String note
) {}

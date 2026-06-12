package org.mj.trip.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TripPlanCreateRequest(
        @NotBlank String startDate,
        @NotBlank String endDate,
        @NotNull Long budgetAmount,
        @NotBlank String region,
        @NotNull Integer companionCount,
        @NotBlank String tripPurpose,
        @NotBlank String transportMode,
        @NotBlank String mealPreference,
        @NotBlank String paceLevel,
        List<String> priorityTypes,
        List<Long> travelStyleIds
) {}

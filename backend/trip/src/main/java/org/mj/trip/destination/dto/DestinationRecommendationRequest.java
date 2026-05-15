package org.mj.trip.destination.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class DestinationRecommendationRequest {

    @NotBlank(message = "여행 목적은 필수입니다.")
    private String tripPurpose;

    @NotNull(message = "여행 스타일 ID 목록은 필수입니다.")
    private List<Long> travelStyleIds;

    @NotBlank(message = "예산 범위는 필수입니다.")
    private String budgetRange;

    @NotBlank(message = "지역은 필수입니다.")
    private String region;

    @NotBlank(message = "계절은 필수입니다.")
    private String season;

    @NotNull(message = "동반자 수는 필수입니다.")
    @Min(value = 1, message = "동반자 수는 1명 이상이어야 합니다.")
    @Max(value = 20, message = "동반자 수는 20명 이하여야 합니다.")
    private Integer companionCount;

    @NotNull(message = "여행 기간은 필수입니다.")
    @Min(value = 1, message = "여행 기간은 1일 이상이어야 합니다.")
    @Max(value = 30, message = "여행 기간은 30일 이하여야 합니다.")
    private Integer durationDays;

    @Builder
    public DestinationRecommendationRequest(String tripPurpose, List<Long> travelStyleIds,
                                            String budgetRange, String region, String season,
                                            Integer companionCount, Integer durationDays) {
        this.tripPurpose = tripPurpose;
        this.travelStyleIds = travelStyleIds;
        this.budgetRange = budgetRange;
        this.region = region;
        this.season = season;
        this.companionCount = companionCount;
        this.durationDays = durationDays;
    }
}

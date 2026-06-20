package org.mj.trip.plan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripPlanCreateRequest(
        @NotNull(message = "시작 날짜를 입력해주세요.")
        LocalDate startDate,

        @NotNull(message = "종료 날짜를 입력해주세요.")
        LocalDate endDate,

        @NotBlank(message = "여행 지역을 입력해주세요.")
        String region,

        @Positive
        @NotNull
        BigDecimal budget,

        @Size(max = 500, message = "내용은 500자 이하로 입력해주세요.")
        String description

        // 여행지에 대한 추천 포인트 목록 (선택적)
//        List<PointRecommendationCreateRequest> points
) {
}

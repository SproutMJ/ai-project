package org.mj.trip.plan.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.plan.dto.TripPlanCreateRequest;
import org.mj.trip.plan.dto.TripPlanCreateResponse;
import org.mj.trip.plan.dto.TripPlanDetailResponse;
import org.mj.trip.plan.dto.TripPlanListResponse;
import org.mj.trip.plan.service.TripPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/trip-plans")
public class TripPlanController {

    private final TripPlanService tripPlanService;

    @PostMapping
    public ResponseEntity<ApiResponse<TripPlanCreateResponse>> createTripPlan(
            @Valid @RequestBody TripPlanCreateRequest request) {

        // 인증이 필요한 API이므로 실제 구현 시 @PreAuthorize("isAuthenticated()") 또는 SecurityContext 검증 추가
        TripPlanCreateResponse response = tripPlanService.createTripPlan(request);

        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @GetMapping("/{tripPlanId}")
    public ResponseEntity<ApiResponse<TripPlanDetailResponse>> getTripPlanDetail(
            @PathVariable Long tripPlanId) {

        TripPlanDetailResponse response = tripPlanService.getTripPlanDetail(tripPlanId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TripPlanListResponse>> listTripPlans(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) LocalDate startDateFrom,
            @RequestParam(required = false) LocalDate startDateTo,
            @RequestParam(required = false) LocalDateTime createdFrom,
            @RequestParam(required = false) LocalDateTime createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        TripPlanListResponse response = tripPlanService.listTripPlans(
                status,
                region,
                startDateFrom,
                startDateTo,
                createdFrom,
                createdTo,
                page - 1, // API 명세서의 page=1은 0-based index의 0에 해당
                size,
                sort,
                order
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{tripPlanId}")
    public ResponseEntity<ApiResponse<Void>> deleteTripPlan(
            @PathVariable("tripPlanId") Long tripPlanId) {
        tripPlanService.deleteTripPlan(tripPlanId);
        return ResponseEntity.noContent().build();
    }
}

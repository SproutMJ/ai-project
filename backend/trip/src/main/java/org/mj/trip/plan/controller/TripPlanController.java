package org.mj.trip.plan.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.plan.dto.request.ScheduleRquestsRequestDto;
import org.mj.trip.plan.dto.request.TripPlanCreateRequest;
import org.mj.trip.plan.dto.response.ScheduleRequestsResponseDto;
import org.mj.trip.plan.dto.response.TripPlanDetailResponse;
import org.mj.trip.plan.service.TripPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/trip-plans")
public class TripPlanController {

    private final TripPlanService tripPlanService;

    @PostMapping
    public ResponseEntity<Void> createTripPlan(
            @Valid @RequestBody TripPlanCreateRequest request,
            HttpServletRequest httpRequest) {

        Long memberId = (Long) httpRequest.getAttribute("memberId");
        tripPlanService.createTripPlan(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{tripPlanId}")
    public ResponseEntity<ApiResponse<TripPlanDetailResponse>> getTripPlanDetail(
            @PathVariable Long tripPlanId) {

        TripPlanDetailResponse response = tripPlanService.getTripPlanDetail(tripPlanId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ScheduleRequestsResponseDto>> listTripPlanRequests(
            @Valid ScheduleRquestsRequestDto scheduleRquestsRequestDto,
            HttpServletRequest httpRequest) {

        Long memberId = (Long) httpRequest.getAttribute("memberId");
        ScheduleRequestsResponseDto dto = tripPlanService.listTripPlanRequests(memberId, scheduleRquestsRequestDto);

        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @DeleteMapping("/{tripPlanId}")
    public ResponseEntity<ApiResponse<Void>> deleteTripPlan(
            @PathVariable("tripPlanId") Long tripPlanId) {
        tripPlanService.deleteTripPlan(tripPlanId);
        return ResponseEntity.noContent().build();
    }
}

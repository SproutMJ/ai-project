package org.mj.trip.destination.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.destination.dto.DestinationRecommendationDetailResponse.DestinationRecommendationDetailData;
import org.mj.trip.destination.dto.DestinationRecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationResponse;
import org.mj.trip.destination.service.DestinationRecommendationService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/v1")
public class DestinationRecommendationController {

    private final DestinationRecommendationService destinationRecommendationService;

    public DestinationRecommendationController(DestinationRecommendationService destinationRecommendationService) {
        this.destinationRecommendationService = destinationRecommendationService;
    }

    @PostMapping("/destination-recommendations")
    public ResponseEntity<ApiResponse<DestinationRecommendationResponse>> createRecommendation(
            HttpServletRequest request,
            @Valid @RequestBody DestinationRecommendationRequest destinationRecommendationRequest) {
        Long memberId = JwtAuthenticationInterceptor.getMemberIdFromRequest(request);
        DestinationRecommendationResponse response = destinationRecommendationService.createRecommendation(
                memberId, destinationRecommendationRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping("/destination-recommendations")
    public ResponseEntity<ApiResponse<List<DestinationRecommendationResponse.RecommendationSummary>>> listRecommendations(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String tripPurpose,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        LocalDateTime createdFromDt = (createdFrom != null && !createdFrom.isEmpty())
                ? LocalDateTime.parse(createdFrom)
                : null;
        LocalDateTime createdToDt = (createdTo != null && !createdTo.isEmpty())
                ? LocalDateTime.parse(createdTo)
                : null;

        Page<DestinationRecommendationResponse.RecommendationSummary> result = destinationRecommendationService.listRecommendations(
                region, tripPurpose, season, createdFromDt, createdToDt, page - 1, size, sort, order);

        ApiResponse<List<DestinationRecommendationResponse.RecommendationSummary>> response = ApiResponse.
                <List<DestinationRecommendationResponse.RecommendationSummary>>success(
                        result.getContent(),
                        ApiResponse.Meta.builder()
                                .page(result.getNumber() + 1)
                                .size(result.getSize())
                                .totalElements(result.getTotalElements())
                                .totalPages(result.getTotalPages())
                                .build()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/destination-recommendations/{recommendationRequestId}")
    public ResponseEntity<ApiResponse<DestinationRecommendationDetailData>> getRecommendationDetail(
            @PathVariable Long recommendationRequestId,
            org.springframework.data.domain.Pageable pageable) {

        DestinationRecommendationDetailData data = destinationRecommendationService.getRecommendationDetail(recommendationRequestId);

        // Create a mock Page with single element to generate pagination metadata
        org.springframework.data.domain.Page<DestinationRecommendationDetailData> mockPage = new org.springframework.data.domain.PageImpl<>(
                java.util.Collections.singletonList(data), pageable, 1);

        ApiResponse<DestinationRecommendationDetailData> response = ApiResponse.<DestinationRecommendationDetailData>success(
                data,
                ApiResponse.Meta.builder()
                        .page(mockPage.getNumber() + 1)
                        .size(mockPage.getSize())
                        .totalElements(mockPage.getTotalElements())
                        .totalPages(mockPage.getTotalPages())
                        .build()
        );

        return ResponseEntity.ok(response);
    }
}

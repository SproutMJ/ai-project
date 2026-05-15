package org.mj.trip.destination.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.dto.ApiResponse;
import org.mj.trip.destination.dto.DestinationRecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationResponse;
import org.mj.trip.destination.service.DestinationRecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

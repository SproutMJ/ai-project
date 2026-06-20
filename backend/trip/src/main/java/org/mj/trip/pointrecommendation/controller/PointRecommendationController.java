
package org.mj.trip.pointrecommendation.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.mj.trip.pointrecommendation.dto.PointRecommendationDetailResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationListRequestDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationListResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestDto;
import org.mj.trip.pointrecommendation.service.PointRecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/recommendations")
@RequiredArgsConstructor
public class PointRecommendationController {

    private final PointRecommendationService pointRecommendationService;

    @PostMapping
    public ResponseEntity<Void> createRecommendation(
            @RequestBody @Valid PointRecommendationRequestDto requestDto,
            HttpServletRequest httpRequest) {

        Long memberId = (Long) httpRequest.getAttribute("memberId");
        pointRecommendationService.createRecommendation(memberId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<PointRecommendationListResponseDto> listRecommendations(
            @Valid PointRecommendationListRequestDto requestDto) {
        Long userId = 1L;
        PointRecommendationListResponseDto response = pointRecommendationService.recommendationRequests(
                requestDto.page(),
                requestDto.size(),
                requestDto.sort(),
                requestDto.order(),
                userId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<PointRecommendationDetailResponseDto> getRecommendationDetail(
            @PathVariable Long requestId) {
        PointRecommendationDetailResponseDto response = pointRecommendationService.getRecommendationDetail(requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{requestId}")
    public ResponseEntity<Void> deleteRecommendationRequest(@PathVariable Long requestId) {
        pointRecommendationService.deleteRecommendationRequest(requestId);
        return ResponseEntity.noContent().build();
    }
}

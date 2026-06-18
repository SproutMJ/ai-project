
package org.mj.trip.pointrecommendation.controller;

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
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class PointRecommendationController {

    private final PointRecommendationService pointRecommendationService;

    @PostMapping
    public ResponseEntity<Void> createRecommendation(
            @RequestBody @Valid PointRecommendationRequestDto requestDto) {
        // TODO: 실제 사용자 ID(memberId)는 인증 필터 또는 SecurityContext에서 가져와야 합니다.
        // 예: Long memberId = SecurityUtils.getCurrentUserId();
        Long memberId = 1L; // 임시 값
        pointRecommendationService.createRecommendation(memberId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<PointRecommendationListResponseDto> listRecommendations(
            @Valid PointRecommendationListRequestDto requestDto) {
        PointRecommendationListResponseDto response = pointRecommendationService.listRecommendations(
                requestDto.page(),
                requestDto.size(),
                requestDto.sort(),
                requestDto.order()
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


package org.mj.trip.pointrecommendation.service;

import lombok.RequiredArgsConstructor;
import org.mj.trip.common.service.AsyncRecommendationService;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.pointrecommendation.domain.PointRecommendation;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRepository;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequestRepository;
import org.mj.trip.pointrecommendation.dto.PointRecommendationDetailResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationListResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointRecommendationService {

    private final PointRecommendationRequestRepository pointRecommendationRequestRepository;
    private final PointRecommendationRepository pointRecommendationRepository;
    private final AsyncRecommendationService asyncRecommendationService;

    @Transactional
    public Long createRecommendation(Long memberId, PointRecommendationRequestDto request) {
        PointRecommendationRequest recommendationRequest = PointRecommendationRequest.builder()
                .userId(memberId)
                .requestText(request.requestText())
                .build();

        PointRecommendationRequest savedRequest = pointRecommendationRequestRepository.save(recommendationRequest);

        // 비동기 스레드에 추천 생성 작업 위임 (이 메서드는 블로킹되지 않고 바로 넘어감)
        asyncRecommendationService.processRecommendationInBackground(
                savedRequest.getId(),
                savedRequest.getRequestText(),
                memberId
        );

        return savedRequest.getId();
    }

    @Transactional(readOnly = true)
    public PointRecommendationListResponseDto recommendationRequests(
            int page, int size, String sort, String order, Long userId) {

        Sort.Direction direction =
                "asc".equalsIgnoreCase(order)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Sort springSort = Sort.by(
                direction, sort
        );

        Pageable pageable = PageRequest.of(
                page, size, springSort
        );

        Page<PointRecommendationRequest> recommendationPage =
                pointRecommendationRequestRepository.findByUserId(userId, pageable);

        List<PointRecommendationRequestResponseDto> summaries =
                recommendationPage.getContent().stream()
                        .map(entity->new PointRecommendationRequestResponseDto(entity.getId(), entity.getRequestText()))
                        .toList();

        PointRecommendationListResponseDto.Meta meta =
                PointRecommendationListResponseDto.Meta.from(recommendationPage);

        return new PointRecommendationListResponseDto(summaries, meta);
    }

    @Transactional(readOnly = true)
    public PointRecommendationDetailResponseDto getRecommendationDetail(Long recommendationRequestId) {
        PointRecommendationRequest request =
                pointRecommendationRequestRepository.findById(recommendationRequestId)
                        .orElseThrow(() -> new ResourceNotFoundException("RecommendationRequest not found: " + recommendationRequestId));

        List<PointRecommendation> recommendations =
                pointRecommendationRepository.findByRequestOrderByRecommendationScoreDesc(request);

        List<PointRecommendationDetailResponseDto.RecommendationInfo> recommendationInfos =
                recommendations.stream()
                        .map(PointRecommendationDetailResponseDto.RecommendationInfo::from)
                        .toList();

        PointRecommendationDetailResponseDto.RequestInfo requestInfo =
                PointRecommendationDetailResponseDto.RequestInfo.from(request);

        return new PointRecommendationDetailResponseDto(requestInfo, recommendationInfos);
    }

    @Transactional
    public void deleteRecommendationRequest(Long recommendationRequestId) {
        PointRecommendationRequest recommendationRequest = pointRecommendationRequestRepository.findById(recommendationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("RecommendationRequest not found: " + recommendationRequestId));

        // Delete associated Recommendation records
        pointRecommendationRepository.deleteByRequestId(recommendationRequestId);

        // Delete the RecommendationRequest record
        pointRecommendationRequestRepository.delete(recommendationRequest);
    }

}

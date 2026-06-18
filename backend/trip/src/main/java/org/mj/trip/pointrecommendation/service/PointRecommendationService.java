
package org.mj.trip.pointrecommendation.service;

import lombok.RequiredArgsConstructor;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.pointrecommendation.domain.PointRecommendation;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRepository;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequestRepository;
import org.mj.trip.pointrecommendation.dto.PointRecommendationDetailResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationListResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestDto;
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

    @Transactional
    public void createRecommendation(Long memberId, PointRecommendationRequestDto request) {
        // 1. RecommendationRequest 저장
        PointRecommendationRequest recommendationRequest = PointRecommendationRequest.builder()
                .userId(memberId)
                .requestText(request.requestText())
                .build();

        recommendationRequest = pointRecommendationRequestRepository.save(recommendationRequest);

        // 2. 추천 생성 (실제로는 AI나 알고리즘이 작동하지만, 여기서는 더미 데이터로 구현)
        generateRecommendations(
                recommendationRequest
        );

    }

    private void generateRecommendations(PointRecommendationRequest request) {
        Long requestId = request.getId();
        Long memberId = request.getUserId();
        String requestText = request.getRequestText();

        throw new RuntimeException("not implemented");
    }

    @Transactional(readOnly = true)
    public PointRecommendationListResponseDto listRecommendations(
            int page, int size, String sort, String order) {

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

        Page<PointRecommendation> recommendationPage =
                pointRecommendationRepository.findAll(pageable);

        List<PointRecommendationListResponseDto.RecommendationSummary> summaries =
                recommendationPage.getContent().stream()
                        .map(PointRecommendationListResponseDto.RecommendationSummary::from)
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

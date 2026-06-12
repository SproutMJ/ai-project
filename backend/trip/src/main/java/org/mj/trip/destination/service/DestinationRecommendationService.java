package org.mj.trip.destination.service;

import lombok.RequiredArgsConstructor;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.destination.domain.Recommendation;
import org.mj.trip.destination.domain.RecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationDetailResponse.DestinationRecommendationDetailData;
import org.mj.trip.destination.dto.DestinationRecommendationDetailResponse.DestinationRecommendationDetailData.RecommendationItem;
import org.mj.trip.destination.dto.DestinationRecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationResponse;
import org.mj.trip.destination.repository.RecommendationRepository;
import org.mj.trip.destination.repository.RecommendationRequestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DestinationRecommendationService {

    private final RecommendationRequestRepository recommendationRequestRepository;
    private final RecommendationRepository recommendationRepository;

    @Transactional
    public DestinationRecommendationResponse createRecommendation(Long memberId, DestinationRecommendationRequest request) {
        // Validate companionCount
        if (request.getCompanionCount() != null && request.getCompanionCount() <= 0) {
            throw new IllegalArgumentException("동반자 수는 1명 이상이어야 합니다.");
        }

        // Validate durationDays
        if (request.getDurationDays() != null && request.getDurationDays() <= 0) {
            throw new IllegalArgumentException("여행 기간은 1일 이상이어야 합니다.");
        }

        // Fallback for null region
//        String region = request.getRegion() != null ? request.getRegion() : "국내";

        // 1. RecommendationRequest 저장
        RecommendationRequest recommendationRequest = RecommendationRequest.builder()
                .memberId(memberId)
                .tripPurpose(request.getTripPurpose())
                .budgetRange(request.getBudgetRange())
                .region(request.getRegion())
                .season(request.getSeason())
                .companionCount(request.getCompanionCount())
                .durationDays(request.getDurationDays())
                .summary(request.getSummary())
                .build();

        recommendationRequest = recommendationRequestRepository.save(recommendationRequest);

        // 2. 추천 생성 (실제로는 AI나 알고리즘이 작동하지만, 여기서는 더미 데이터로 구현)
        List<Recommendation> recommendations = generateRecommendations(
                recommendationRequest.getRecommendationRequestId(),
                request, request.getRegion()
        );

        // 생성된 추천 목록 저장 (ID 발급을 위해 필수)
        recommendations = recommendationRepository.saveAll(recommendations).stream().toList();

        // 4. 응답 생성
        List<DestinationRecommendationResponse.Recommendation> responseRecommendations = recommendations.stream()
                .map(rec -> {
                    return DestinationRecommendationResponse.Recommendation.builder()
                            .recommendationId(rec.getId())
                            .destinationId(rec.getDestinationId())
                            .destinationName(rec.getDestinationName())
                            .score(rec.getScore())
                            .rankOrder(rec.getRankOrder())
                            .reasonSummary(rec.getReasonSummary())
                            .build();
                })
                .toList();

        return DestinationRecommendationResponse.builder()
                .recommendationRequestId(recommendationRequest.getRecommendationRequestId())
                .recommendations(responseRecommendations)
                .createdAt(recommendationRequest.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<DestinationRecommendationResponse.RecommendationSummary> listRecommendations(
            String region, String tripPurpose, String season,
            LocalDateTime createdFrom, LocalDateTime createdTo,
            int page, int size, String sort, String order) {
        // 정렬 설정
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = (sort != null && !sort.isEmpty()) ? sort : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // 동적 필터링
        Specification<RecommendationRequest> spec = Specification.where(null);
        if (region != null && !region.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("region"), region));
        }
        if (tripPurpose != null && !tripPurpose.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("tripPurpose"), tripPurpose));
        }
        if (season != null && !season.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("season"), season));
        }
        if (createdFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        Page<RecommendationRequest> requestPage = recommendationRequestRepository.findAll(spec, pageable);

        return requestPage.map(req -> DestinationRecommendationResponse.RecommendationSummary.builder()
                .recommendationRequestId(req.getRecommendationRequestId())
                .summary(req.getSummary())
                .createdAt(req.getCreatedAt())
                .region(req.getRegion())
                .tripPurpose(req.getTripPurpose())
                .budgetRange(req.getBudgetRange())
                .season(req.getSeason())
                .companionCount(req.getCompanionCount())
                .durationDays(req.getDurationDays())
                .build());
    }

    private List<Recommendation> generateRecommendations(Long recommendationRequestId, DestinationRecommendationRequest request, String region) {
        List<Recommendation> recommendations = new ArrayList<>();
        Random random = new Random();

        String[] destinations;
        if (region.equals("일본")) {
            destinations = new String[]{"도쿄", "오사카", "교토", "후쿠오카", "호쿠라쿠"};
        } else if (region.equals("태국")) {
            destinations = new String[]{"방콕", "푸켓", "치앙마이", "파타야", "코사부"};
        } else if (region.equals("베트남")) {
            destinations = new String[]{"하노이", "호찌민", "다낭", "닌트란", "후에"};
        } else {
            destinations = new String[]{"서울", "부산", "제주도", "강릉", "진해"};
        }

        for (int i = 0; i < 5; i++) {
            double score = 80.0 + (random.nextDouble() * 20);
            score = Math.round(score * 10.0) / 10.0;

            String reasonSummary = generateReasonSummary(request, destinations[i]);

            Recommendation recommendation = Recommendation.builder()
                    .recommendationRequestId(recommendationRequestId)
                    .destinationId(500L + i)
                    .destinationName(destinations[i])
                    .score(score)
                    .rankOrder(i + 1)
                    .reasonSummary(reasonSummary)
                    .build();

            recommendations.add(recommendation);
        }

        // score 기준으로 정렬
        recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        // rankOrder 재할당
        for (int i = 0; i < recommendations.size(); i++) {
            Recommendation rec = recommendations.get(i);
            recommendations.set(i, Recommendation.builder()
                    .recommendationRequestId(rec.getRecommendationRequestId())
                    .destinationId(rec.getDestinationId())
                    .destinationName(rec.getDestinationName())
                    .score(rec.getScore())
                    .rankOrder(i + 1)
                    .reasonSummary(rec.getReasonSummary())
                    .build());
        }

        return recommendations;
    }

    private String generateReasonSummary(DestinationRecommendationRequest request, String destination) {
        StringBuilder sb = new StringBuilder();
        sb.append(destination).append("은(는) ");

        if (request.getTripPurpose().contains("휴식")) {
            sb.append("휴식과 ").append(request.getCompanionCount()).append("명의 동반자와 ").append(request.getDurationDays()).append("일 일정에 적합합니다.");
        } else if (request.getTripPurpose().contains("쇼핑")) {
            sb.append("쇼핑을 즐기기 좋고, ").append(request.getDurationDays()).append("일 일정에 적합합니다.");
        } else {
            sb.append(request.getTripPurpose()).append("을(를) 즐기기 좋고, ").append(request.getDurationDays()).append("일 일정에 적합합니다.");
        }

        return sb.toString();
    }

    @Transactional(readOnly = true)
    public DestinationRecommendationDetailData getRecommendationDetail(Long recommendationRequestId) {
        // 1. RecommendationRequest 조회
        RecommendationRequest recommendationRequest = recommendationRequestRepository.findById(recommendationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("RecommendationRequest not found: " + recommendationRequestId));

        // 2. Recommendation 목록 조회
        List<Recommendation> recommendations = recommendationRepository.findByRecommendationRequestIdOrderByRankOrderAsc(recommendationRequestId);

        // 3. 응답 매핑
        List<RecommendationItem> responseRecommendations = recommendations.stream()
                .map(rec -> {
                    return RecommendationItem.builder()
                            .recommendationId(rec.getId())
                            .destinationId(rec.getDestinationId())
                            .destinationName(rec.getDestinationName())
                            .score(rec.getScore())
                            .rankOrder(rec.getRankOrder())
                            .reasonSummary(rec.getReasonSummary())
                            .build();
                })
                .toList();

        return DestinationRecommendationDetailData.builder()
                .tripPurpose(recommendationRequest.getTripPurpose())
                .budgetRange(recommendationRequest.getBudgetRange())
                .region(recommendationRequest.getRegion())
                .season(recommendationRequest.getSeason())
                .createdAt(recommendationRequest.getCreatedAt())
                .recommendations(responseRecommendations)
                .build();
    }

    @Transactional
    public void deleteRecommendationRequest(Long recommendationRequestId) {
        RecommendationRequest recommendationRequest = recommendationRequestRepository.findById(recommendationRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("RecommendationRequest not found: " + recommendationRequestId));

        // Delete associated Recommendation records
        recommendationRepository.deleteByRecommendationRequestId(recommendationRequestId);

        // Delete the RecommendationRequest record
        recommendationRequestRepository.delete(recommendationRequest);
    }

}

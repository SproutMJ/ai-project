package org.mj.trip.plan.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mj.trip.common.service.AsyncRecommendationService;
import org.mj.trip.plan.domain.RouteRecommendation;
import org.mj.trip.plan.domain.RouteRecommendationRepository;
import org.mj.trip.plan.domain.RouteScheduleItem;
import org.mj.trip.plan.domain.ScheduleRequest;
import org.mj.trip.plan.domain.ScheduleRequestRepository;
import org.mj.trip.plan.dto.request.ScheduleRquestsRequestDto;
import org.mj.trip.plan.dto.request.TripPlanCreateRequest;
import org.mj.trip.plan.dto.response.ScheduleRequestResponseDto;
import org.mj.trip.plan.dto.response.ScheduleRequestsResponseDto;
import org.mj.trip.plan.dto.response.TripPlanDetailResponse;
import org.mj.trip.plan.dto.response.TripPlanListItemResponse;
import org.mj.trip.plan.dto.response.TripPlanListResponse;
import org.mj.trip.plan.dto.response.TripPlanPointDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class TripPlanService {

    private final RouteRecommendationRepository routeRecommendationRepository;
    private final ScheduleRequestRepository scheduleRequestRepository;
    private final AsyncRecommendationService asyncRecommendationService;


    @Transactional
    public Long createTripPlan(Long memberId, TripPlanCreateRequest request) {

        // 1. RecommendationRequest 저장
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .userId(memberId)
                .requestText(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .region(request.region())
                .budget(request.budget())
                .build();

        ScheduleRequest saved = scheduleRequestRepository.save(scheduleRequest);

        // 2. 비동기 스레드에 추천 생성 작업 위임 (이 메서드는 블로킹되지 않고 바로 넘어감)
        asyncRecommendationService.processPlanningInBackground(memberId, saved);

        // 3. 클라이언트에게는 저장된 요청의 ID만 즉시 반환
        return saved.getId();
    }

    public TripPlanDetailResponse getTripPlanDetail(Long tripPlanId) {
        RouteRecommendation entity = routeRecommendationRepository.findWithItemsById(tripPlanId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행 일정을 찾을 수 없습니다. id=" + tripPlanId));

        // RouteDaySchedule -> RouteScheduleItem 내부에서 장소(NODE) 데이터만 추출하여 DTO로 변환
        List<TripPlanPointDetailResponse> pointResponses = entity.getDaySchedules().stream()
                .flatMap(day -> day.getScheduleItems().stream())
                .filter(item -> item.getItemType() == RouteScheduleItem.ItemType.NODE)
                .map(item -> new TripPlanPointDetailResponse(
                        item.getId(),
                        item.getName(),
                        item.getRegion(),
                        item.getShortComment(),
                        entity.getRecommendationScore()
                ))
                .toList();

        // Entity -> DetailResponse DTO 반환
        return new TripPlanDetailResponse(
                entity.getId(),
                entity.getName(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getRegion(),
                entity.getShortComment(),
                pointResponses,
                "ACTIVE", // TODO: Entity에 status 추가 후 매핑
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    @Transactional
    public void deleteTripPlan(Long tripPlanId) {
        RouteRecommendation routeRecommendation = routeRecommendationRepository.findById(tripPlanId)
                .orElseThrow(() -> new IllegalArgumentException("해당 여행 일정을 찾을 수 없습니다. id=" + tripPlanId));

        routeRecommendationRepository.delete(routeRecommendation);
    }

    public ScheduleRequestsResponseDto listTripPlanRequests(Long userId, @Valid ScheduleRquestsRequestDto scheduleRquestsRequestDto) {
        Sort.Direction direction =
                "asc".equalsIgnoreCase(scheduleRquestsRequestDto.order())
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Sort springSort = Sort.by(
                direction, scheduleRquestsRequestDto.sort()
        );

        Pageable pageable = PageRequest.of(
                scheduleRquestsRequestDto.page(), scheduleRquestsRequestDto.size(), springSort
        );

        Page<ScheduleRequest> scheduleRequests =
                scheduleRequestRepository.findByUserId(userId, pageable);

        List<ScheduleRequestResponseDto> summaries =
                scheduleRequests.getContent().stream()
                        .map(entity -> new ScheduleRequestResponseDto(entity.getId(), entity.getUserId(), entity.getRequestText(), entity.getStartDate(), entity.getEndDate(), entity.getRegion(), entity.getBudget()))
                        .toList();

        ScheduleRequestsResponseDto.Meta meta = ScheduleRequestsResponseDto.Meta.from(scheduleRequests);

        return new ScheduleRequestsResponseDto(summaries, meta);
    }
}

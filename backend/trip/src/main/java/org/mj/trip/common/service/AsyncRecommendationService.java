package org.mj.trip.common.service;

import lombok.RequiredArgsConstructor;
import org.mj.trip.plan.domain.RouteDaySchedule;
import org.mj.trip.plan.domain.RouteRecommendation;
import org.mj.trip.plan.domain.RouteRecommendationRepository;
import org.mj.trip.plan.domain.RouteScheduleItem;
import org.mj.trip.plan.domain.ScheduleRequest;
import org.mj.trip.plan.domain.ScheduleRequestRepository;
import org.mj.trip.pointrecommendation.domain.PointRecommendation;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRepository;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequestRepository;
import org.mj.trip.pointrecommendation.dto.AiRecommendationDto;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsyncRecommendationService {

    private final AiRequestClient agentClient;
    private final PointRecommendationRepository pointRecommendationRepository;
    private final PointRecommendationRequestRepository requestRepository;

    private final RouteRecommendationRepository routeRecommendationRepository;

    @Async
    @Transactional
    public void processRecommendationInBackground(Long requestId, String requestText, Long memberId) {
        try {
            // 1. FastAPI (smolagent) 호출 (여기서 시간이 오래 걸림)
            List<AiRecommendationDto> aiRecommendations = agentClient.getRecommendations(requestText);

            System.out.println(aiRecommendations);

            // 2. Request 엔티티 조회
            PointRecommendationRequest request = requestRepository.findById(requestId)
                    .orElseThrow(() -> new RuntimeException("Request not found"));

            // 3. 엔티티 변환 및 저장
            List<PointRecommendation> entities = aiRecommendations.stream()
                    .map(dto -> PointRecommendation.builder()
                            .request(request)
                            .userId(memberId)
                            .name(dto.name())
                            .recommendationScore(dto.recommendationScore())
                            .shortComment(dto.shortComment())
                            .type(dto.type())
                            .region(dto.region())
                            .keyword(dto.keyword())
                            .theme(dto.theme())
                            .budget(dto.budget())
                            .requiredTime(dto.requiredTime())
                            .howToGo(dto.howToGo())
                            .recommendedPartySize(dto.recommendedPartySize())
                            .weather(dto.weather())
                            .language(dto.language())
                            .disadvantage(dto.disadvantage())
                            .description(dto.description())
                            .build())
                    .toList();

            pointRecommendationRepository.saveAll(entities);

            // TIP: 실제 서비스에서는 PointRecommendationRequest에 상태 필드(status)를 추가하여
            // 여기서 'COMPLETED'로 업데이트해주는 것이 좋습니다.

        } catch (Exception e) {
            // 실패 시 처리 로직 (예: 상태를 'FAILED'로 변경)
            throw e;
        }
    }

    @Async
    @Transactional
    public void processPlanningInBackground(Long memberId, ScheduleRequest scheduleRequest) {

        try {
            // 1. FastAPI (smolagent) 호출
            // 요청 텍스트뿐만 아니라 날짜, 예산, 지역 등을 함께 넘겨 정교한 플랜을 받아옵니다.
            AiRouteRecommendationDto aiResponse = agentClient.getRoutePlan(
                    scheduleRequest.getRequestText(),
                    scheduleRequest.getStartDate(),
                    scheduleRequest.getEndDate(),
                    scheduleRequest.getRegion(),
                    scheduleRequest.getBudget()
            );

            System.out.println(aiResponse);

            // 2. 최상위 루트 추천 엔티티(RouteRecommendation) 생성
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .requestId(scheduleRequest.getId())
                    .userId(memberId)
                    .name(aiResponse.name())
                    .recommendationScore(aiResponse.recommendationScore())
                    .startDate(scheduleRequest.getStartDate())
                    .endDate(scheduleRequest.getEndDate())
                    .shortComment(aiResponse.shortComment())
                    .budget(aiResponse.budget())
                    .region(aiResponse.region())
                    .build();

            // 3. 일차별 스케줄(RouteDaySchedule) 매핑
            if (aiResponse.daySchedules() != null) {
                for (AiRouteDayScheduleDto dayDto : aiResponse.daySchedules()) {

                    RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                            .dayNumber(dayDto.dayNumber())
                            .name(dayDto.name())
                            .build();

                    // 4. 상세 스케줄 아이템(RouteScheduleItem - NODE / EDGE) 매핑
                    if (dayDto.scheduleItems() != null) {
                        for (AiRouteScheduleItemDto itemDto : dayDto.scheduleItems()) {

                            RouteScheduleItem scheduleItem = RouteScheduleItem.builder()
                                    .sequence(itemDto.sequence())
                                    .itemType(itemDto.itemType())
                                    .nodeType(itemDto.nodeType())
                                    .name(itemDto.name())
                                    .region(itemDto.region())
                                    .shortComment(itemDto.shortComment())
                                    .budget(itemDto.budget())
                                    .startTime(itemDto.startTime())
                                    .endTime(itemDto.endTime())
                                    .transportType(itemDto.transportType())
                                    .travelMinutes(itemDto.travelMinutes())
                                    .description(itemDto.description())
                                    .build();

                            // DaySchedule에 Item 추가 (양방향 연관관계 세팅됨)
                            daySchedule.addScheduleItem(scheduleItem);
                        }
                    }
                    // RouteRecommendation에 DaySchedule 추가 (양방향 연관관계 및 정렬 세팅됨)
                    routeRecommendation.addDaySchedule(daySchedule);
                }
            }

            // 5. DB 저장
            // CascadeType.ALL 설정 덕분에 최상위 객체만 저장하면 하위 객체(일차, 세부일정) 모두 자동 INSERT 됩니다.
            routeRecommendationRepository.save(routeRecommendation);

        } catch (Exception e) {
            throw e;
        }
    }

    public record AiRouteRecommendationDto(
            String name,
            Double recommendationScore,
            String shortComment,
            String budget,
            String region,
            List<AiRouteDayScheduleDto> daySchedules
    ) {}

    public record AiRouteDayScheduleDto(
            Integer dayNumber,
            String name,
            List<AiRouteScheduleItemDto> scheduleItems
    ) {}

    public record AiRouteScheduleItemDto(
            Integer sequence,
            RouteScheduleItem.ItemType itemType,
            String nodeType,
            String name,
            String region,
            String shortComment,
            String budget,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String transportType,
            Integer travelMinutes,
            String description
    ) {}
}

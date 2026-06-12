package org.mj.trip.plan.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.plan.domain.TripPlan;
import org.mj.trip.plan.dto.TripPlanCreateRequest;
import org.mj.trip.plan.dto.TripPlanCreateResponse;
import org.mj.trip.plan.dto.TripPlanDetailResponse;
import org.mj.trip.plan.dto.TripPlanListResponse;
import org.mj.trip.plan.repository.TripPlanRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TripPlanService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TripPlanRepository tripPlanRepository;

    public TripPlanService(TripPlanRepository tripPlanRepository) {
        this.tripPlanRepository = tripPlanRepository;
    }


    @Transactional
    public TripPlanCreateResponse createTripPlan(TripPlanCreateRequest request) {
        // 1. 도메인 엔티티 매핑
        TripPlan tripPlan = TripPlan.create(
                1L,
                LocalDate.of(2026, 05, 04),
                LocalDate.of(2026, 05, 06),
                BigDecimal.valueOf(1_000_000L),
                "도쿄",
                2,
                "맛집탐방",
                "대중교통",
                "현지식",
                "여유롭게",
                List.of("ATTRACTION", "RESTAURANT")
        );
        // ... request 값 매핑 로직

        // 2. AI 일정 생성 로직 (외부 AI API 호출 또는 로컬 생성 로직)
        // 실제 구현 시 여기에서 LLM API 호출 및 응답 파싱 수행

        // 3. Repository에 저장
        TripPlan savedTripPlan = tripPlanRepository.save(tripPlan);

        // 4. 응답 DTO 매핑 (명세 기준)
        return new TripPlanCreateResponse(
                savedTripPlan.getId(),
                3001L,
                "DRAFT",
                new TripPlanCreateResponse.SummaryResponse(
                        "3일 동안 도쿄 핵심 지역을 여유롭게 즐기는 일정입니다.",
                        List.of("맛집 중심", "대중교통 활용", "관광과 쇼핑 균형")
                ),
                List.of(
                        new TripPlanCreateResponse.DayResponse(
                                1,
                                "2026-05-04",
                                List.of(
                                        new TripPlanCreateResponse.ItemResponse("10:00", "12:00", "ATTRACTION", "시부야 스크램블")
                                )
                        )
                ),
                "2026-04-21T10:00:00Z"
        );
    }

    @Transactional(readOnly = true)
    public TripPlanListResponse listTripPlans(
            String status,
            String region,
            LocalDate startDateFrom,
            LocalDate startDateTo,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            int page,
            int size,
            String sort,
            String order) {

        // 정렬 방향 설정 (기본값 DESC)
        Sort.Direction direction = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String sortBy = (sort != null && !sort.isEmpty()) ? sort : "createdAt";
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Repository의 applyFilters 메소드 사용
        Page<TripPlan> tripPlanPage = tripPlanRepository.applyFilters(
                status,
                region,
                startDateFrom,
                startDateTo,
                createdFrom,
                createdTo,
                pageable
        );

        // 빈 결과일 경우 totalPages를 0으로 설정
        if (tripPlanPage.getContent().isEmpty()) {
            return TripPlanListResponse.from(
                    tripPlanPage.getContent(),
                    new PageImpl<>(tripPlanPage.getContent(), pageable, 0)
            );
        }

        return TripPlanListResponse.from(tripPlanPage.getContent(), tripPlanPage);
    }

    @Transactional
    public TripPlanDetailResponse getTripPlanDetail(Long tripPlanId) {
        TripPlan tripPlan = tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("일정 정보를 찾을 수 없습니다. id: " + tripPlanId));

        // planData JSON 파싱
        List<Map<String, Object>> days = new ArrayList<>();
        if (tripPlan.getPlanData() != null && !tripPlan.getPlanData().isEmpty()) {
            try {
                days = objectMapper.readValue(tripPlan.getPlanData(), new TypeReference<List<Map<String, Object>>>() {
                });
            } catch (Exception e) {
                // JSON 파싱 실패 시 빈 리스트 사용
                days = new ArrayList<>();
            }
        }

        List<TripPlanDetailResponse.Day> dayResponses = days.stream().map(dayMap -> {
            Integer dayNo = (Integer) dayMap.get("dayNo");
            String planDateStr = (String) dayMap.get("planDate");
            LocalDate planDate = (planDateStr != null) ? LocalDate.parse(planDateStr) : null;
            List<Map<String, Object>> itemsMap = (List<Map<String, Object>>) dayMap.get("items");

            List<TripPlanDetailResponse.Item> items = (itemsMap != null) ? itemsMap.stream().map(itemMap ->
                    TripPlanDetailResponse.Item.builder()
                            .itemId(Long.valueOf(String.valueOf(itemMap.get("itemId"))))
                            .startTime((String) itemMap.get("startTime"))
                            .endTime((String) itemMap.get("endTime"))
                            .itemType((String) itemMap.get("itemType"))
                            .placeName((String) itemMap.get("placeName"))
                            .build()
            ).toList() : List.of();

            return TripPlanDetailResponse.Day.builder()
                    .dayNo(dayNo)
                    .planDate(planDate)
                    .items(items)
                    .build();
        }).toList();

        return TripPlanDetailResponse.builder()
                .tripPlanId(tripPlan.getId())
                .status(tripPlan.getStatus())
                .request(TripPlanDetailResponse.Request.builder()
                        .startDate(tripPlan.getStartDate())
                        .endDate(tripPlan.getEndDate())
                        .budgetAmount(tripPlan.getBudgetAmount())
                        .build())
                .summary(TripPlanDetailResponse.Summary.builder()
                        .text(tripPlan.getSummaryText())
                        .build())
                .days(dayResponses)
                .build();
    }

    @Transactional
    public void deleteTripPlan(Long tripPlanId) {
        TripPlan tripPlan = tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId)
                .orElseThrow(() -> new ResourceNotFoundException("삭제할 일정 정보를 찾을 수 없습니다. id: " + tripPlanId));

        tripPlanRepository.delete(tripPlan);
    }
}

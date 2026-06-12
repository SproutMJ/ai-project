package org.mj.trip.plan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.plan.domain.TripPlan;
import org.mj.trip.plan.dto.TripPlanCreateRequest;
import org.mj.trip.plan.dto.TripPlanCreateResponse;
import org.mj.trip.plan.dto.TripPlanDetailResponse;
import org.mj.trip.plan.dto.TripPlanListResponse;
import org.mj.trip.plan.repository.TripPlanRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@DisplayName("TripPlanService 테스트")
@ExtendWith(MockitoExtension.class)
class TripPlanServiceTest {

    @Mock
    private TripPlanRepository tripPlanRepository;

    private TripPlanService tripPlanService;

    private TripPlan tripPlan1;
    private TripPlan tripPlan2;

    @BeforeEach
    void setUp() {
        tripPlanService = new TripPlanService(tripPlanRepository);
        tripPlan1 = TripPlan.builder()
                .id(1L)
                .tripPlanRequestId(1001L)
                .status("DRAFT")
                .summaryText("도쿄 여행 일정입니다.")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 5))
                .budgetAmount(BigDecimal.valueOf(1000000))
                .region("도쿄")
                .companionCount(2)
                .tripPurpose("맛집탐방")
                .transportMode("대중교통")
                .mealPreference("현지식")
                .paceLevel("여유롭게")
                .priorityTypes("ATTRACTION,RESTAURANT")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0, 0))
                .build();

        tripPlan2 = TripPlan.builder()
                .id(2L)
                .tripPlanRequestId(1002L)
                .status("ACTIVE")
                .summaryText("오사카 여행 일정입니다.")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 3))
                .budgetAmount(BigDecimal.valueOf(800000))
                .region("오사카")
                .companionCount(1)
                .tripPurpose("관광")
                .transportMode("택시")
                .mealPreference("한식")
                .paceLevel("빠르게")
                .priorityTypes("SHOPPING")
                .createdAt(LocalDateTime.of(2026, 4, 22, 14, 30, 0))
                .build();
    }

    private TripPlanCreateRequest createRequest() {
        return new TripPlanCreateRequest(
                "2026-05-04",
                "2026-05-06",
                1_000_000L,
                "도쿄",
                2,
                "맛집탐방",
                "대중교통",
                "현지식",
                "여유롭게",
                List.of("ATTRACTION", "RESTAURANT"),
                List.of(1L, 2L)
        );
    }

    private TripPlan createTripPlanWithId(Long id) {
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

        try {
            Field idField = TripPlan.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(tripPlan, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return tripPlan;
    }

    @Test
    @DisplayName("성공: 여행 일정 생성")
    void createTripPlan_success() {
        // given
        TripPlanCreateRequest request = createRequest();
        TripPlan savedTripPlan = createTripPlanWithId(1L);

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenReturn(savedTripPlan);

        // when
        TripPlanCreateResponse response = tripPlanService.createTripPlan(request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.tripPlanId());
        assertEquals(3001L, response.tripPlanRequestId());
        assertEquals("DRAFT", response.status());
        assertEquals("2026-04-21T10:00:00Z", response.createdAt());

        verify(tripPlanRepository, times(1)).save(any(TripPlan.class));
    }

    @Test
    @DisplayName("성공: 여행 일정 생성 응답의 요약 정보 검증")
    void createTripPlan_success_verifySummary() {
        // given
        TripPlanCreateRequest request = createRequest();
        TripPlan savedTripPlan = createTripPlanWithId(10L);

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenReturn(savedTripPlan);

        // when
        TripPlanCreateResponse response = tripPlanService.createTripPlan(request);

        // then
        assertNotNull(response.summary());
        assertEquals(
                "3일 동안 도쿄 핵심 지역을 여유롭게 즐기는 일정입니다.",
                response.summary().text()
        );
        assertEquals(3, response.summary().keyPoints().size());
        assertTrue(response.summary().keyPoints().contains("맛집 중심"));
        assertTrue(response.summary().keyPoints().contains("대중교통 활용"));
        assertTrue(response.summary().keyPoints().contains("관광과 쇼핑 균형"));
    }

    @Test
    @DisplayName("성공: 여행 일정 생성 응답의 일자별 일정 검증")
    void createTripPlan_success_verifyDays() {
        // given
        TripPlanCreateRequest request = createRequest();
        TripPlan savedTripPlan = createTripPlanWithId(20L);

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenReturn(savedTripPlan);

        // when
        TripPlanCreateResponse response = tripPlanService.createTripPlan(request);

        // then
        assertNotNull(response.days());
        assertEquals(1, response.days().size());

        TripPlanCreateResponse.DayResponse day = response.days().get(0);
        assertEquals(1, day.dayNo());
        assertEquals("2026-05-04", day.planDate());

        assertNotNull(day.items());
        assertEquals(1, day.items().size());

        TripPlanCreateResponse.ItemResponse item = day.items().get(0);
        assertEquals("10:00", item.startTime());
        assertEquals("12:00", item.endTime());
        assertEquals("ATTRACTION", item.itemType());
        assertEquals("시부야 스크램블", item.placeName());
    }

    @Test
    @DisplayName("성공: Repository 저장 호출 검증")
    void createTripPlan_success_verifyRepositorySaveCall() {
        // given
        TripPlanCreateRequest request = createRequest();
        TripPlan savedTripPlan = createTripPlanWithId(30L);

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenReturn(savedTripPlan);

        // when
        tripPlanService.createTripPlan(request);

        // then
        verify(tripPlanRepository, times(1)).save(any(TripPlan.class));
        verifyNoMoreInteractions(tripPlanRepository);
    }

    @Test
    @DisplayName("실패: Repository 저장 시 DataAccessException 발생")
    void createTripPlan_failure_repositoryThrowsDataAccessException() {
        // given
        TripPlanCreateRequest request = createRequest();

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenThrow(new DataAccessException("DB error") {
                });

        // when & then
        assertThrows(DataAccessException.class, () ->
                tripPlanService.createTripPlan(request)
        );

        verify(tripPlanRepository, times(1)).save(any(TripPlan.class));
    }

    @Test
    @DisplayName("실패: Repository가 null을 반환하는 경우 NullPointerException 발생")
    void createTripPlan_failure_repositoryReturnsNull() {
        // given
        TripPlanCreateRequest request = createRequest();

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenReturn(null);

        // when & then
        assertThrows(NullPointerException.class, () ->
                tripPlanService.createTripPlan(request)
        );

        verify(tripPlanRepository, times(1)).save(any(TripPlan.class));
    }

    @Test
    @DisplayName("성공: 요청 값이 null이어도 현재 구현에서는 저장 후 고정 응답 반환")
    void createTripPlan_success_nullRequest() {
        // given
        TripPlan savedTripPlan = createTripPlanWithId(40L);

        when(tripPlanRepository.save(any(TripPlan.class)))
                .thenReturn(savedTripPlan);

        // when
        TripPlanCreateResponse response = tripPlanService.createTripPlan(null);

        // then
        assertNotNull(response);
        assertEquals(40L, response.tripPlanId());
        assertEquals("DRAFT", response.status());

        verify(tripPlanRepository, times(1)).save(any(TripPlan.class));
    }

    @Test
    @DisplayName("성공: 전체 일정 목록 조회 - 페이지네이션 기본값 적용")
    void listTripPlans_success_basicPagination() {
        // given
        List<TripPlan> tripPlans = List.of(tripPlan1, tripPlan2);
        // Pageable 객체를 명시적으로 생성하여 PageImpl에 전달
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TripPlan> page = new PageImpl<>(tripPlans, pageable, tripPlans.size());
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, null, null, null, null, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getMeta().getPage()).isEqualTo(1);
        assertThat(response.getMeta().getSize()).isEqualTo(20);
        assertThat(response.getMeta().getTotalElements()).isEqualTo(2);
        assertThat(response.getMeta().getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 상태 필터 적용 (DRAFT)")
    void listTripPlans_success_filterByStatus() {
        // given
        List<TripPlan> draftPlans = List.of(tripPlan1);
        Page<TripPlan> page = new PageImpl<>(draftPlans);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                "DRAFT", null, null, null, null, null, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 지역 필터 적용 (도쿄)")
    void listTripPlans_success_filterByRegion() {
        // given
        List<TripPlan> tokyoPlans = List.of(tripPlan1);
        Page<TripPlan> page = new PageImpl<>(tokyoPlans);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, "도쿄", null, null, null, null, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getRegion()).isEqualTo("도쿄");
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 시작 날짜 범위 필터 적용")
    void listTripPlans_success_filterByStartDateRange() {
        // given
        LocalDate startDateFrom = LocalDate.of(2026, 5, 1);
        LocalDate startDateTo = LocalDate.of(2026, 5, 31);
        List<TripPlan> plansInRange = List.of(tripPlan1);
        Page<TripPlan> page = new PageImpl<>(plansInRange);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, startDateFrom, startDateTo, null, null, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(1);
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 생성 일시 범위 필터 적용")
    void listTripPlans_success_filterByCreatedAtRange() {
        // given
        LocalDateTime createdFrom = LocalDateTime.of(2026, 4, 22, 0, 0, 0);
        LocalDateTime createdTo = LocalDateTime.of(2026, 4, 23, 0, 0, 0);
        List<TripPlan> plansInRange = List.of(tripPlan2);
        Page<TripPlan> page = new PageImpl<>(plansInRange);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, null, null, createdFrom, createdTo, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getCreatedAt()).isBetween(createdFrom, createdTo);
    }


    @Test
    @DisplayName("성공: 일정 목록 조회 - 정렬 내림차순 (createdAt desc)")
    void listTripPlans_success_sortDesc() {
        // given
        // 내림차순 정렬 결과: tripPlan2 (4/22) > tripPlan1 (4/21)
        List<TripPlan> tripPlans = List.of(tripPlan2, tripPlan1);
        Page<TripPlan> page = new PageImpl<>(tripPlans);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, null, null, null, null, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(2);
        // createdAt 기준 내림차순: tripPlan2 (4/22) > tripPlan1 (4/21)
        assertThat(response.getData().get(0).getCreatedAt()).isAfter(response.getData().get(1).getCreatedAt());
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 정렬 오름차순 (createdAt asc)")
    void listTripPlans_success_sortAsc() {
        // given
        List<TripPlan> tripPlans = List.of(tripPlan1, tripPlan2);
        Page<TripPlan> page = new PageImpl<>(tripPlans);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, null, null, null, null, 1, 20, "createdAt", "asc"
        );

        // then
        assertThat(response.getData()).hasSize(2);
        // createdAt 기준 오름차순: tripPlan1 (4/21) < tripPlan2 (4/22)
        assertThat(response.getData().get(0).getCreatedAt()).isBefore(response.getData().get(1).getCreatedAt());
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 빈 결과 반환")
    void listTripPlans_success_emptyResults() {
        // given
        Page<TripPlan> emptyPage = Page.empty(Pageable.unpaged());
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(emptyPage);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, null, null, null, null, 1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).isEmpty();
        assertThat(response.getMeta().getTotalElements()).isEqualTo(0);
        assertThat(response.getMeta().getTotalPages()).isEqualTo(0);
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 여러 필터 조합")
    void listTripPlans_success_multipleFilters() {
        // given
        List<TripPlan> filteredPlans = List.of(tripPlan1);
        Page<TripPlan> page = new PageImpl<>(filteredPlans);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                "DRAFT", "도쿄", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 12, 31),
                LocalDateTime.of(2026, 1, 1, 0, 0, 0), LocalDateTime.of(2026, 12, 31, 23, 59, 59),
                1, 20, "createdAt", "desc"
        );

        // then
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getStatus()).isEqualTo("DRAFT");
        assertThat(response.getData().get(0).getRegion()).isEqualTo("도쿄");
    }

    @Test
    @DisplayName("성공: 응답 DTO 매핑 검증")
    void listTripPlans_success_verifyResponseDto() {
        // given
        List<TripPlan> tripPlans = List.of(tripPlan1);
        Page<TripPlan> page = new PageImpl<>(tripPlans);
        when(tripPlanRepository.applyFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(page);

        // when
        TripPlanListResponse response = tripPlanService.listTripPlans(
                null, null, null, null, null, null, 1, 20, "createdAt", "desc"
        );
        TripPlanListResponse.TripPlanItem item = response.getData().get(0);

        // then
        assertThat(item.getTripPlanId()).isEqualTo(1L);
        assertThat(item.getRegion()).isEqualTo("도쿄");
        assertThat(item.getStatus()).isEqualTo("DRAFT");
        assertThat(item.getSummaryText()).isEqualTo("도쿄 여행 일정입니다.");
        assertThat(item.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 21, 10, 0, 0));
    }


    @Test
    @DisplayName("성공: 여행 일정 상세 조회 - planData JSON 파싱 포함")
    void getTripPlanDetail_success_withPlanData() {
        // given
        Long tripPlanId = 1L;

        // planData JSON 생성
        String planDataJson = """
                    [
                        {
                            "dayNo": 1,
                            "planDate": "2026-05-04",
                            "items": [
                                {
                                    "itemId": 1,
                                    "startTime": "10:00",
                                    "endTime": "12:00",
                                    "itemType": "ATTRACTION",
                                    "placeName": "시부야 스크램블"
                                }
                            ]
                        },
                        {
                            "dayNo": 2,
                            "planDate": "2026-05-05",
                            "items": []
                        }
                    ]
                    """;

        // TripPlan 엔티티 생성 (planData 포함)
        TripPlan tripPlan = TripPlan.builder()
                .id(tripPlanId)
                .tripPlanRequestId(1001L)
                .status("ACTIVE")
                .summaryText("도쿄 여행 일정입니다.")
                .startDate(LocalDate.of(2026, 5, 4))
                .endDate(LocalDate.of(2026, 5, 6))
                .budgetAmount(BigDecimal.valueOf(1000000))
                .region("도쿄")
                .companionCount(2)
                .tripPurpose("맛집탐방")
                .transportMode("대중교통")
                .mealPreference("현지식")
                .paceLevel("여유롭게")
                .priorityTypes("ATTRACTION,RESTAURANT")
                .planData(planDataJson) // JSON 데이터 설정
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0, 0))
                .build();

        when(tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId))
                .thenReturn(Optional.of(tripPlan));

        // when
        TripPlanDetailResponse response = tripPlanService.getTripPlanDetail(tripPlanId);

        // then
        assertNotNull(response);
        assertEquals(tripPlanId, response.getTripPlanId());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals("도쿄 여행 일정입니다.", response.getSummary().getText());

        // days 검증
        assertThat(response.getDays()).hasSize(2);

        // 첫 번째 day 검증
        TripPlanDetailResponse.Day day1 = response.getDays().get(0);
        assertEquals(1, day1.getDayNo());
        assertEquals(LocalDate.of(2026, 5, 4), day1.getPlanDate());
        assertThat(day1.getItems()).hasSize(1);

        TripPlanDetailResponse.Item item1 = day1.getItems().get(0);
        assertEquals(1L, item1.getItemId());
        assertEquals("10:00", item1.getStartTime());
        assertEquals("12:00", item1.getEndTime());
        assertEquals("ATTRACTION", item1.getItemType());
        assertEquals("시부야 스크램블", item1.getPlaceName());

        // 두 번째 day 검증 (빈 items)
        TripPlanDetailResponse.Day day2 = response.getDays().get(1);
        assertEquals(2, day2.getDayNo());
        assertEquals(LocalDate.of(2026, 5, 5), day2.getPlanDate());
        assertTrue(day2.getItems().isEmpty());

        verify(tripPlanRepository).findByIdAndDeletedAtIsNull(tripPlanId);
    }

    @Test
    @DisplayName("성공: 여행 일정 상세 조회 - planData가 null인 경우")
    void getTripPlanDetail_success_nullPlanData() {
        // given
        Long tripPlanId = 2L;

        TripPlan tripPlan = TripPlan.builder()
                .id(tripPlanId)
                .tripPlanRequestId(1002L)
                .status("DRAFT")
                .summaryText("예정 일정입니다.")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 6, 3))
                .budgetAmount(BigDecimal.valueOf(800000))
                .region("오사카")
                .companionCount(1)
                .tripPurpose("관광")
                .transportMode("택시")
                .mealPreference("한식")
                .paceLevel("빠르게")
                .priorityTypes("SHOPPING")
                .planData(null) // planData null
                .createdAt(LocalDateTime.of(2026, 4, 22, 14, 30, 0))
                .build();

        when(tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId))
                .thenReturn(Optional.of(tripPlan));

        // when
        TripPlanDetailResponse response = tripPlanService.getTripPlanDetail(tripPlanId);

        // then
        assertNotNull(response);
        assertEquals(tripPlanId, response.getTripPlanId());
        assertEquals("DRAFT", response.getStatus());
        assertTrue(response.getDays().isEmpty()); // planData가 null이면 빈 리스트

        verify(tripPlanRepository).findByIdAndDeletedAtIsNull(tripPlanId);
    }

    @Test
    @DisplayName("성공: 여행 일정 상세 조회 - planData JSON 파싱 실패 시 빈 리스트 반환")
    void getTripPlanDetail_success_invalidJsonPlanData() {
        // given
        Long tripPlanId = 3L;

        TripPlan tripPlan = TripPlan.builder()
                .id(tripPlanId)
                .tripPlanRequestId(1003L)
                .status("ACTIVE")
                .summaryText("잘못된 JSON 데이터")
                .startDate(LocalDate.of(2026, 7, 1))
                .endDate(LocalDate.of(2026, 7, 3))
                .budgetAmount(BigDecimal.valueOf(500000))
                .region("제주도")
                .companionCount(2)
                .tripPurpose("휴양")
                .transportMode("자차")
                .mealPreference("한식")
                .paceLevel("여유롭게")
                .priorityTypes("ATTRACTION")
                .planData("invalid json data") // 유효하지 않은 JSON
                .createdAt(LocalDateTime.of(2026, 4, 23, 9, 0, 0))
                .build();

        when(tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId))
                .thenReturn(Optional.of(tripPlan));

        // when
        TripPlanDetailResponse response = tripPlanService.getTripPlanDetail(tripPlanId);

        // then
        assertNotNull(response);
        assertEquals(tripPlanId, response.getTripPlanId());
        assertTrue(response.getDays().isEmpty()); // JSON 파싱 실패 시 빈 리스트

        verify(tripPlanRepository).findByIdAndDeletedAtIsNull(tripPlanId);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 일정 ID 조회 시 ResourceNotFoundException 발생")
    void getTripPlanDetail_failure_notFound() {
        // given
        Long tripPlanId = 999L;

        when(tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId))
                .thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () ->
                tripPlanService.getTripPlanDetail(tripPlanId)
        );

        verify(tripPlanRepository).findByIdAndDeletedAtIsNull(tripPlanId);
    }

    @Test
    @DisplayName("일정 삭제 성공")
    void deleteTripPlan_success() {
        // given
        Long tripPlanId = 1L;
        TripPlan tripPlan = createMockTripPlan();
        when(tripPlanRepository.findByIdAndDeletedAtIsNull(tripPlanId)).thenReturn(Optional.of(tripPlan));
        doNothing().when(tripPlanRepository).delete(tripPlan);

        // when
        tripPlanService.deleteTripPlan(tripPlanId);

        // then
        verify(tripPlanRepository, times(1)).delete(tripPlan);
    }

    @Test
    @DisplayName("일정 삭제 실패 - 존재하지 않는 일정")
    void deleteTripPlan_notFound() {
        // given
        Long nonExistentId = 999L;
        when(tripPlanRepository.findByIdAndDeletedAtIsNull(nonExistentId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> tripPlanService.deleteTripPlan(nonExistentId));
        verify(tripPlanRepository, never()).delete(any());
    }

    @Test
    @DisplayName("일정 삭제 실패 - 이미 삭제된 일정")
    void deleteTripPlan_alreadyDeleted() {
        // given
        Long alreadyDeletedId = 888L;
        when(tripPlanRepository.findByIdAndDeletedAtIsNull(alreadyDeletedId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> tripPlanService.deleteTripPlan(alreadyDeletedId));
        verify(tripPlanRepository, never()).delete(any());
    }

    private TripPlan createMockTripPlan() {
        return TripPlan.builder()
                .id(1L)
                .memberId(1L)
                .startDate(LocalDate.of(2026, 5, 4))
                .endDate(LocalDate.of(2026, 5, 6))
                .budgetAmount(BigDecimal.valueOf(1_000_000))
                .region("도쿄")
                .companionCount(2)
                .tripPurpose("맛집탐방")
                .status("DRAFT")
                .createdAt(LocalDateTime.now())
                .build();
    }
}

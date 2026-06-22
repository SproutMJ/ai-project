package org.mj.trip.plan.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mj.trip.common.service.AsyncRecommendationService;
import org.mj.trip.plan.domain.RouteDaySchedule;
import org.mj.trip.plan.domain.RouteRecommendation;
import org.mj.trip.plan.domain.RouteRecommendationRepository;
import org.mj.trip.plan.domain.RouteScheduleItem;
import org.mj.trip.plan.domain.ScheduleRequest;
import org.mj.trip.plan.domain.ScheduleRequestRepository;
import org.mj.trip.plan.dto.request.ScheduleRquestsRequestDto;
import org.mj.trip.plan.dto.request.TripPlanCreateRequest;
import org.mj.trip.plan.dto.response.ScheduleRequestsResponseDto;
import org.mj.trip.plan.dto.response.TripPlanDetailResponse;
import org.mj.trip.plan.dto.response.TripPlanPointDetailResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripPlanServiceTest {

    @Mock
    private RouteRecommendationRepository routeRecommendationRepository;

    @Mock
    private ScheduleRequestRepository scheduleRequestRepository;

    @Mock
    private AsyncRecommendationService asyncRecommendationService;

    @InjectMocks
    private TripPlanService tripPlanService;

    private RouteRecommendation sampleEntity;
    private RouteDaySchedule sampleDaySchedule;
    private RouteScheduleItem sampleItem;

    @BeforeEach
    void setUp() {
        // 테스트용 Entity 설정
        sampleEntity = RouteRecommendation.builder()
                .id(1L)
                .userId(1L)
                .requestId(1L)
                .name("테스트 여행지")
                .shortComment("설명입니다")
                .region("제주도")
                .recommendationScore(9.5)
                .budget("100만원")
                .build();

        // 테스트용 ScheduleItem 설정 (NODE 타입)
        sampleItem = RouteScheduleItem.builder()
                .id(1L)
                .name("테스트 장소")
                .region("제주도")
                .shortComment("장소 설명")
                .itemType(RouteScheduleItem.ItemType.NODE)
                .build();

        // 테스트용 DaySchedule 설정
        sampleDaySchedule = RouteDaySchedule.builder()
                .id(1L)
                .dayNumber(1)
                .scheduleItems(List.of(sampleItem))
                .build();

        // Entity에 DaySchedule 연결
        sampleEntity.getDaySchedules().add(sampleDaySchedule);
    }

    @DisplayName("여행 일정 생성 성공")
    @Test
    void createTripPlan_Success() {
        // given
        TripPlanCreateRequest request = new TripPlanCreateRequest(
                LocalDate.of(24, 7, 1),
                LocalDate.of(24, 7, 5),
                "제주도",
                BigDecimal.valueOf(1000000),
                "여행 계획을 만들어줘"
        );

        // ScheduleRequest 엔티티를 위한 Mock 객체 생성 (ID가 1L이라고 가정)
        ScheduleRequest mockScheduleRequest = ScheduleRequest.builder()
                .id(1L)
                .userId(1L)
                .requestText("여행 계획을 만들어줘")
                .build();

        when(scheduleRequestRepository.save(any(ScheduleRequest.class)))
                .thenReturn(mockScheduleRequest);

        Long memberId = 1L;
        // when
        Long id = tripPlanService.createTripPlan(memberId, request);

        // then
        assertThat(id)
                .isNotNull()
                .isEqualTo(1L); // mockScheduleRequest의 ID와 일치해야 합니다.

        verify(scheduleRequestRepository, times(1)).save(any(ScheduleRequest.class));
    }

    @DisplayName("여행 일정 상세 조회 성공")
    @Test
    void getTripPlanDetail_Success() {
        // given
        when(routeRecommendationRepository.findWithItemsById(1L))
                .thenReturn(Optional.of(sampleEntity));

        // when
        TripPlanDetailResponse response = tripPlanService.getTripPlanDetail(1L);

        // then
        assertThat(response.tripPlanId()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("테스트 여행지");
        assertThat(response.region()).isEqualTo("제주도");
        assertThat(response.points()).hasSize(1);

        TripPlanPointDetailResponse point = response.points().get(0);
        assertThat(point.pointId()).isEqualTo(1L);
        assertThat(point.pointName()).isEqualTo("테스트 장소");
        assertThat(point.address()).isEqualTo("제주도");
    }

    @DisplayName("여행 일정 상세 조회 실패 - 존재하지 않는 ID")
    @Test
    void getTripPlanDetail_NotFound() {
        // given
        when(routeRecommendationRepository.findWithItemsById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tripPlanService.getTripPlanDetail(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 여행 일정을 찾을 수 없습니다");
    }

    @DisplayName("여행 일정 목록 조회 성공")
    @Test
    void listTripPlans_Success() {
        // given
        // 수정: 서비스 코드에서 생성하는 Sort 조건(createdAt DESC)을 동일하게 반영해야 합니다.
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(0, 10, sort);

        ScheduleRequest mockScheduleRequest = ScheduleRequest.builder()
                .id(1L)
                .userId(1L)
                .requestText("테스트 여행 요청")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .region("제주도")
                .budget(BigDecimal.valueOf(1000000))
                .build();

        Page<ScheduleRequest> scheduleRequestPage = new PageImpl<>(
                List.of(mockScheduleRequest),
                pageable,
                1L
        );

        when(scheduleRequestRepository.findByUserId(eq(1L), eq(pageable)))
                .thenReturn(scheduleRequestPage);

        // when
        ScheduleRequestsResponseDto response = tripPlanService.listTripPlanRequests(
                1L,
                new ScheduleRquestsRequestDto(0, 10, "createdAt", "desc")
        );

        // then
        assertAll("여행 일정 목록 페이징 응답 검증",
                // 1. Meta 데이터 검증
                () -> assertThat(response.meta().page()).isEqualTo(1),
                () -> assertThat(response.meta().size()).isEqualTo(10),
                () -> assertThat(response.meta().totalElements()).isEqualTo(1L),
                () -> assertThat(response.meta().totalPages()).isEqualTo(1),

                // 2. 리스트 데이터 검증
                () -> assertThat(response.scheduleRequests()).hasSize(1)
        );

        // verify: scheduleRequestRepository.findByUserId가 호출되었는지 확인
        verify(scheduleRequestRepository, times(1)).findByUserId(eq(1L), eq(pageable));
    }

    @DisplayName("여행 일정 삭제 성공")
    @Test
    void deleteTripPlan_Success() {
        // given
        when(routeRecommendationRepository.findById(1L))
                .thenReturn(Optional.of(sampleEntity));

        // when
        tripPlanService.deleteTripPlan(1L);

        // then
        verify(routeRecommendationRepository, times(1)).delete(sampleEntity);
    }

    @DisplayName("여행 일정 삭제 실패 - 존재하지 않는 ID")
    @Test
    void deleteTripPlan_NotFound() {
        // given
        when(routeRecommendationRepository.findById(999L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> tripPlanService.deleteTripPlan(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 여행 일정을 찾을 수 없습니다");
    }
}

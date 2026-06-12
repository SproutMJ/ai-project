
package org.mj.trip.plan.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.auth.token.JwtTokenProvider;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.plan.dto.TripPlanDetailResponse;
import org.mj.trip.plan.dto.TripPlanListResponse;
import org.mj.trip.plan.service.TripPlanService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripPlanController.class)
@DisplayName("TripPlanController 테스트")
class TripPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TripPlanService tripPlanService;

    private TripPlanListResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = createMockResponse();

        // primitive int 파라미터에는 any()가 아니라 anyInt()를 사용해야 합니다.
        doReturn(mockResponse).when(tripPlanService).listTripPlans(
                any(),         // status
                any(),         // region
                any(),         // startDateFrom
                any(),         // startDateTo
                any(),         // createdFrom
                any(),         // createdTo
                anyInt(),      // page
                anyInt(),      // size
                anyString(),   // sort
                anyString()    // order
        );
    }

    @Test
    @DisplayName("성공: 전체 일정 목록 조회 - 기본 파라미터")
    void listTripPlans_success_defaultParams() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "createdAt")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isNotEmpty()) // data 객체가 존재해야 함
                .andExpect(jsonPath("$.data.data").isArray()) // 실제 배열은 data.data에 있음
                .andExpect(jsonPath("$.data.data[0].tripPlanId").value(1))
                .andExpect(jsonPath("$.data.data[0].region").value("도쿄"))
                .andExpect(jsonPath("$.data.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.meta.page").value(1))
                .andExpect(jsonPath("$.data.meta.size").value(20))
                .andExpect(jsonPath("$.data.meta.totalElements").value(2));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 상태 필터 적용")
    void listTripPlans_success_filterByStatus() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].status").value("DRAFT"));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 지역 필터 적용")
    void listTripPlans_success_filterByRegion() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("region", "도쿄"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].region").value("도쿄"));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 날짜 범위 필터 적용")
    void listTripPlans_success_filterByDateRange() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("startDateFrom", "2026-05-01")
                        .param("startDateTo", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 생성 일시 범위 필터 적용")
    void listTripPlans_success_filterByCreatedAtRange() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("createdFrom", "2026-04-20T00:00:00")
                        .param("createdTo", "2026-04-24T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 정렬 오름차순")
    void listTripPlans_success_sortAsc() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 페이지네이션 파라미터")
    void listTripPlans_success_pagination() throws Exception {
        TripPlanListResponse mockResponse2 = createMockResponse();

        Mockito.reset(tripPlanService);
        doReturn(mockResponse2).when(tripPlanService).listTripPlans(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                eq(1),      // page=2 -> controller에서 page - 1 = 1
                eq(10),
                anyString(),
                anyString()
        );

        mockMvc.perform(get("/v1/trip-plans")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("성공: 일정 목록 조회 - 모든 필터 조합")
    void listTripPlans_success_allFilters() throws Exception {
        mockMvc.perform(get("/v1/trip-plans")
                        .param("status", "DRAFT")
                        .param("region", "도쿄")
                        .param("startDateFrom", "2026-05-01")
                        .param("startDateTo", "2026-05-31")
                        .param("createdFrom", "2026-04-01T00:00:00")
                        .param("createdTo", "2026-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.data.data[0].region").value("도쿄"));
    }

    @Test
    @DisplayName("성공: 빈 결과 반환")
    void listTripPlans_success_emptyResults() throws Exception {
        TripPlanListResponse emptyResponse = TripPlanListResponse.from(
                Collections.emptyList(),
                Page.empty(Pageable.unpaged())
        );

        Mockito.reset(tripPlanService);
        doReturn(emptyResponse).when(tripPlanService).listTripPlans(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                anyInt(),
                anyString(),
                anyString()
        );

        mockMvc.perform(get("/v1/trip-plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isEmpty()) // 빈 배열은 data.data에 위치
                .andExpect(jsonPath("$.data.meta.totalElements").value(0));
    }

    private TripPlanListResponse createMockResponse() {
        TripPlanListResponse.TripPlanItem item1 = new TripPlanListResponse.TripPlanItem(
                1L, "도쿄", "DRAFT", "도쿄 여행 일정입니다.",
                LocalDateTime.of(2026, 4, 21, 10, 0, 0)
        );
        TripPlanListResponse.TripPlanItem item2 = new TripPlanListResponse.TripPlanItem(
                2L, "오사카", "ACTIVE", "오사카 여행 일정입니다.",
                LocalDateTime.of(2026, 4, 22, 14, 30, 0)
        );

        TripPlanListResponse.Meta meta = new TripPlanListResponse.Meta(1, 20, 2, 1);
        return new TripPlanListResponse(List.of(item1, item2), meta);
    }

    @Test
    @DisplayName("성공: 일정 상세 조회")
    void getTripPlanDetail_success() throws Exception {
        // given
        Long tripPlanId = 1L;

        TripPlanDetailResponse.Request request = TripPlanDetailResponse.Request.builder()
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 5))
                .budgetAmount(new BigDecimal("1000000"))
                .build();

        TripPlanDetailResponse.Summary summary = TripPlanDetailResponse.Summary.builder()
                .text("도쿄 여행 요약입니다.")
                .build();

        TripPlanDetailResponse.Item item = TripPlanDetailResponse.Item.builder()
                .itemId(1L)
                .startTime("10:00")
                .endTime("12:00")
                .itemType("ACTIVITY")
                .placeName("센주 공항")
                .build();

        TripPlanDetailResponse.Day day = TripPlanDetailResponse.Day.builder()
                .dayNo(1)
                .planDate(LocalDate.of(2026, 5, 1))
                .items(List.of(item))
                .build();

        TripPlanDetailResponse response = TripPlanDetailResponse.builder()
                .tripPlanId(tripPlanId)
                .status("ACTIVE")
                .request(request)
                .summary(summary)
                .days(List.of(day))
                .build();

        when(tripPlanService.getTripPlanDetail(tripPlanId)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/v1/trip-plans/{tripPlanId}", tripPlanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tripPlanId").value(1))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.request.startDate").value("2026-05-01"))
                .andExpect(jsonPath("$.data.request.endDate").value("2026-05-05"))
                .andExpect(jsonPath("$.data.summary.text").value("도쿄 여행 요약입니다."))
                .andExpect(jsonPath("$.data.days").isArray())
                .andExpect(jsonPath("$.data.days[0].dayNo").value(1))
                .andExpect(jsonPath("$.data.days[0].items[0].placeName").value("센주 공항"));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 일정 상세 조회")
    void getTripPlanDetail_notFound() throws Exception {
        // given
        Long nonExistentId = 999L;

        doThrow(new ResourceNotFoundException("일정을 찾을 수 없습니다."))
                .when(tripPlanService).getTripPlanDetail(nonExistentId);

        // when & then
        mockMvc.perform(get("/v1/trip-plans/{tripPlanId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("실패: 잘못된 일정 ID 형식")
    void getTripPlanDetail_invalidId() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/trip-plans/{tripPlanId}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패: 음수 일정 ID")
    void getTripPlanDetail_negativeId() throws Exception {
        // given
        Long negativeId = -1L;

        doThrow(new IllegalArgumentException("잘못된 일정 ID입니다."))
                .when(tripPlanService).getTripPlanDetail(negativeId);

        // when & then
        mockMvc.perform(get("/v1/trip-plans/{tripPlanId}", negativeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }


    @Test
    @DisplayName("성공: 일정 삭제 - 204 No Content")
    void deleteTripPlan_success() throws Exception {
        // given
        Long tripPlanId = 1L;

        doNothing().when(tripPlanService).deleteTripPlan(tripPlanId);

        // when & then
        mockMvc.perform(delete("/v1/trip-plans/{tripPlanId}", tripPlanId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패: 삭제할 일정 없음 - 404 Not Found")
    void deleteTripPlan_notFound() throws Exception {
        // given
        Long nonExistentId = 999L;

        doThrow(new ResourceNotFoundException("삭제할 일정 정보를 찾을 수 없습니다. id: " + nonExistentId))
                .when(tripPlanService).deleteTripPlan(nonExistentId);

        // when & then
        mockMvc.perform(delete("/v1/trip-plans/{tripPlanId}", nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("실패: 잘못된 일정 ID 형식 - 400 Bad Request")
    void deleteTripPlan_invalidId() throws Exception {
        // when & then
        mockMvc.perform(delete("/v1/trip-plans/{tripPlanId}", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("실패: 음수 일정 ID - 400 Bad Request")
    void deleteTripPlan_negativeId() throws Exception {
        // given
        Long negativeId = -1L;

        doThrow(new IllegalArgumentException("잘못된 일정 ID입니다."))
                .when(tripPlanService).deleteTripPlan(negativeId);

        // when & then
        mockMvc.perform(delete("/v1/trip-plans/{tripPlanId}", negativeId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }
}

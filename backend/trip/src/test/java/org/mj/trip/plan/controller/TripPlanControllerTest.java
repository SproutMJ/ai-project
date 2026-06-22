package org.mj.trip.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.config.WebConfig;
import org.mj.trip.common.exception.GlobalExceptionHandler;
import org.mj.trip.plan.dto.request.TripPlanCreateRequest;
import org.mj.trip.plan.dto.response.ScheduleRequestResponseDto;
import org.mj.trip.plan.dto.response.ScheduleRequestsResponseDto;
import org.mj.trip.plan.dto.response.TripPlanDetailResponse;
import org.mj.trip.plan.service.TripPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TripPlanController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
class TripPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TripPlanService tripPlanService;

    @MockBean
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @BeforeEach
    void setUp() throws Exception {
        // 인터셉터가 요청 객체에 memberId를 설정하도록 stub
        lenient().when(jwtAuthenticationInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any()))
                .thenAnswer(invocation -> {
                    HttpServletRequest request = invocation.getArgument(0);
                    request.setAttribute("memberId", 1L);
                    return true;
                });
    }

    @Test
    @DisplayName("여행 일정 생성 API 테스트")
    void createTripPlan() throws Exception {
        // given
        TripPlanCreateRequest request = new TripPlanCreateRequest(
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 10, 4),
                "제주도",
                BigDecimal.valueOf(1000000),
                "가족과 함께하는 힐링 여행"
        );

        Long memberId = 1L;

        given(tripPlanService.createTripPlan(anyLong(), any(TripPlanCreateRequest.class)))
                .willReturn(memberId);

        // when & then
        mockMvc.perform(post("/v1/trip-plans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("여행 일정 상세 조회 API 테스트")
    void getTripPlanDetail() throws Exception {
        // given
        Long tripPlanId = 1L;
        TripPlanDetailResponse response = new TripPlanDetailResponse(
                tripPlanId,
                "제주도 3박 4일 여행",
                LocalDate.of(2023, 10, 1),
                LocalDate.of(2023, 10, 4),
                "제주도",
                "가족과 함께하는 힐링 여행",
                Collections.emptyList(),
                "ACTIVE",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        given(tripPlanService.getTripPlanDetail(tripPlanId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/v1/trip-plans/{tripPlanId}", tripPlanId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tripPlanId").value(1L))
                .andExpect(jsonPath("$.data.title").value("제주도 3박 4일 여행"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("여행 일정 목록 페이징 조회 API 테스트")
    void listTripPlans() throws Exception {
        // given
        LocalDate startDate = LocalDate.of(2023, 10, 1);
        LocalDate endDate = LocalDate.of(2023, 10, 4);

        // 1. 단일 응답 객체 생성
        ScheduleRequestResponseDto item = new ScheduleRequestResponseDto(
                1L,
                1L,
                "제주도 3박 4일 여행", // title 대신 requestText 필드에 값 주입
                startDate,
                endDate,
                "제주도",
                BigDecimal.valueOf(1000000)
        );

        // 2. 페이징 메타 데이터 생성 (page: 1, size: 20, totalElements: 1, totalPages: 1)
        ScheduleRequestsResponseDto.Meta meta = new ScheduleRequestsResponseDto.Meta(1, 20, 1L, 1);

        // 3. 서비스가 최종적으로 반환해야 할 감싸진 응답(List + Meta) DTO 생성
        ScheduleRequestsResponseDto responseDto = new ScheduleRequestsResponseDto(List.of(item), meta);

        // 서비스 Mocking
        given(tripPlanService.listTripPlanRequests(any(), any()))
                .willReturn(responseDto);


        // when & then
        mockMvc.perform(get("/v1/trip-plans")
                        .param("page", "1")
                        .param("size", "20")
                        .param("sort", "createdAt")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                // JSON Path 검증 - data 객체 내부의 구조에 맞게 수정
                .andExpect(jsonPath("$.data.meta.page").value(1))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1))
                // 수정: $.scheduleRequests -> $.data.scheduleRequests
                .andExpect(jsonPath("$.data.scheduleRequests[0].id").value(1L))
                .andExpect(jsonPath("$.data.scheduleRequests[0].requestText").value("제주도 3박 4일 여행"));
    }

    @Test
    @DisplayName("여행 일정 삭제 API 테스트")
    void deleteTripPlan() throws Exception {
        // given
        Long tripPlanId = 1L;
        doNothing().when(tripPlanService).deleteTripPlan(tripPlanId);

        // when & then
        mockMvc.perform(delete("/v1/trip-plans/{tripPlanId}", tripPlanId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                // 컨트롤러에서 ResponseEntity.noContent().build(); 를 반환하므로 204 No Content 기대
                .andExpect(status().isNoContent());
    }
}

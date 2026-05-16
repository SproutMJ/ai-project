package org.mj.trip.destination.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.config.WebConfig;
import org.mj.trip.common.exception.GlobalExceptionHandler;
import org.mj.trip.destination.dto.DestinationRecommendationRequest;
import org.mj.trip.destination.dto.DestinationRecommendationResponse;
import org.mj.trip.destination.service.DestinationRecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("DestinationRecommendationController 테스트")
@WebMvcTest(DestinationRecommendationController.class)
@Import({WebConfig.class, GlobalExceptionHandler.class})
class DestinationRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DestinationRecommendationService destinationRecommendationService;

    @MockBean
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("여행지 추천 생성 성공")
    void createRecommendation_success() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L, 2L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        DestinationRecommendationResponse.Recommendation responseRecommendation = DestinationRecommendationResponse.Recommendation.builder()
                .recommendationId(1L)
                .destinationId(500L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄는 휴식과 2명의 동반자와 5일 일정에 적합합니다.")
                .reasons(List.of(
                        DestinationRecommendationResponse.Reason.builder()
                                .type("BUDGET_MATCH")
                                .text("예산 범위 (저예산) 내에서 충분히 구성 가능합니다.")
                                .build(),
                        DestinationRecommendationResponse.Reason.builder()
                                .type("SEASON_MATCH")
                                .text("여름 계절에 적합한 활동과 경관을 즐길 수 있습니다.")
                                .build(),
                        DestinationRecommendationResponse.Reason.builder()
                                .type("DURATION_MATCH")
                                .text("5일 일정으로 충분히 즐길 수 있는 코스입니다.")
                                .build()
                ))
                .build();

        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(1L)
                .recommendations(List.of(responseRecommendation))
                .createdAt(LocalDateTime.now())
                .build();

        when(destinationRecommendationService.createRecommendation(any(Long.class), any(DestinationRecommendationRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendationRequestId").value(1))
                .andExpect(jsonPath("$.data.recommendations[0].destinationName").value("도쿄"))
                .andExpect(jsonPath("$.data.recommendations[0].score").value(95.5))
                .andExpect(jsonPath("$.data.recommendations[0].rankOrder").value(1))
                .andExpect(jsonPath("$.data.recommendations[0].reasons.length()").value(3));
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 필수 필드 누락 (여행 목적)")
    void createRecommendation_fail_missingTripPurpose() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 필수 필드 누락 (예산 범위)")
    void createRecommendation_fail_missingBudgetRange() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 필수 필드 누락 (지역)")
    void createRecommendation_fail_missingRegion() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 동반자 수 유효성 검사 오류 (1 미만)")
    void createRecommendation_fail_companionCountLessThanOne() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(0)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("companionCount"))
                .andExpect(jsonPath("$.error.details[0].reason").value("동반자 수는 1명 이상이어야 합니다."));
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 동반자 수 유효성 검사 오류 (20 초과)")
    void createRecommendation_fail_companionCountMoreThanTwenty() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(21)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("companionCount"))
                .andExpect(jsonPath("$.error.details[0].reason").value("동반자 수는 20명 이하여야 합니다."));
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 여행 기간 유효성 검사 오류 (1일 미만)")
    void createRecommendation_fail_durationDaysLessThanOne() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(0)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("durationDays"))
                .andExpect(jsonPath("$.error.details[0].reason").value("여행 기간은 1일 이상이어야 합니다."));
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - 여행 기간 유효성 검사 오류 (30일 초과)")
    void createRecommendation_fail_durationDaysMoreThanThirty() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(31)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details[0].field").value("durationDays"))
                .andExpect(jsonPath("$.error.details[0].reason").value("여행 기간은 30일 이하여야 합니다."));
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - travelStyleIds null")
    void createRecommendation_fail_travelStyleIdsNull() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(null)
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - companionCount null")
    void createRecommendation_fail_companionCountNull() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(null)
                .durationDays(5)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("여행지 추천 생성 실패 - durationDays null")
    void createRecommendation_fail_durationDaysNull() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("휴식")
                .travelStyleIds(List.of(1L))
                .budgetRange("저예산")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(null)
                .build();

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.message").exists());
    }

    @Test
    @DisplayName("여러 지역별 추천 성공 - 일본")
    void createRecommendation_success_japan() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("쇼핑")
                .travelStyleIds(List.of(1L))
                .budgetRange("고예산")
                .region("일본")
                .season("봄")
                .companionCount(3)
                .durationDays(7)
                .build();

        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(2L)
                .recommendations(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        when(destinationRecommendationService.createRecommendation(any(Long.class), any(DestinationRecommendationRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("여러 지역별 추천 성공 - 태국")
    void createRecommendation_success_thailand() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("액티비티")
                .travelStyleIds(List.of(2L, 3L))
                .budgetRange("중예산")
                .region("태국")
                .season("겨울")
                .companionCount(4)
                .durationDays(10)
                .build();

        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(3L)
                .recommendations(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        when(destinationRecommendationService.createRecommendation(any(Long.class), any(DestinationRecommendationRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("여러 지역별 추천 성공 - 베트남")
    void createRecommendation_success_vietnam() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("문화체험")
                .travelStyleIds(List.of(4L))
                .budgetRange("저예산")
                .region("베트남")
                .season("가을")
                .companionCount(1)
                .durationDays(14)
                .build();

        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(4L)
                .recommendations(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        when(destinationRecommendationService.createRecommendation(any(Long.class), any(DestinationRecommendationRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("여러 지역별 추천 성공 - 국내")
    void createRecommendation_success_domestic() throws Exception {
        // given
        DestinationRecommendationRequest request = DestinationRecommendationRequest.builder()
                .tripPurpose("맛집탐방")
                .travelStyleIds(List.of(5L, 6L))
                .budgetRange("중예산")
                .region("국내")
                .season("여름")
                .companionCount(2)
                .durationDays(3)
                .build();

        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(5L)
                .recommendations(List.of())
                .createdAt(LocalDateTime.now())
                .build();

        when(destinationRecommendationService.createRecommendation(any(Long.class), any(DestinationRecommendationRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ================== listRecommendations 테스트 ====================

    @Test
    @DisplayName("추천 목록 조회 성공 - 기본 페이지네이션")
    void listRecommendations_success_default() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(1L)
                        .summary("첫 번째 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build(),
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(2L)
                        .summary("두 번째 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 1, 2, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build(),
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(3L)
                        .summary("세 번째 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 1, 3, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                3
        );

        when(destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 20, "createdAt", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].recommendationRequestId").value(1))
                .andExpect(jsonPath("$.data[0].summary").value("첫 번째 여행 계획"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(20))
                .andExpect(jsonPath("$.meta.totalElements").value(3))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - region 필터 적용")
    void listRecommendations_success_withRegion() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(10L)
                        .summary("일본 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 3, 15, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        when(destinationRecommendationService.listRecommendations(
                "일본", null, null, null, null, 1, 20, "createdAt", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("region", "일본")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].recommendationRequestId").value(10))
                .andExpect(jsonPath("$.data[0].summary").value("일본 여행 계획"));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - tripPurpose 필터 적용")
    void listRecommendations_success_withTripPurpose() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(20L)
                        .summary("맛집탐방 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 5, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        when(destinationRecommendationService.listRecommendations(
                null, "맛집탐방", null, null, null, 1, 20, "createdAt", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("tripPurpose", "맛집탐방")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].summary").value("맛집탐방 여행 계획"));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - season 필터 적용")
    void listRecommendations_success_withSeason() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(30L)
                        .summary("여름 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 6, 15, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        when(destinationRecommendationService.listRecommendations(
                null, null, "여름", null, null, 1, 20, "createdAt", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("season", "여름")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].summary").value("여름 여행 계획"));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - createdFrom 및 createdTo 필터 적용")
    void listRecommendations_success_withCreatedDateRange() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(40L)
                        .summary("특정 기간 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 7, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        LocalDateTime createdFrom = LocalDateTime.of(2024, 6, 1, 0, 0);
        LocalDateTime createdTo = LocalDateTime.of(2024, 7, 31, 23, 59);

        when(destinationRecommendationService.listRecommendations(
                null, null, null, createdFrom, createdTo, 1, 20, "createdAt", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("createdFrom", createdFrom.toString())
                        .param("createdTo", createdTo.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].summary").value("특정 기간 여행 계획"));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - 복수 필터 적용")
    void listRecommendations_success_withMultipleFilters() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(50L)
                        .summary("일본 쇼핑 여름 여행")
                        .createdAt(LocalDateTime.of(2024, 8, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        when(destinationRecommendationService.listRecommendations(
                "일본", "쇼핑", "여름", null, null, 1, 20, "createdAt", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("region", "일본")
                        .param("tripPurpose", "쇼핑")
                        .param("season", "여름")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].recommendationRequestId").value(50));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - 빈 결과")
    void listRecommendations_success_emptyResult() throws Exception {
        // given
        Page<DestinationRecommendationResponse.RecommendationSummary> emptyPage = Page.empty();

        when(destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 20, "createdAt", "desc"))
                .thenReturn(emptyPage);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(jsonPath("$.meta.totalElements").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(1));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - 커서 페이지네이션")
    void listRecommendations_success_cursorPagination() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(100L)
                        .summary("페이지 2 계획")
                        .createdAt(LocalDateTime.of(2024, 9, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(1, 10),
                15L
        );

        when(destinationRecommendationService.listRecommendations(
                any(), any(), any(), any(), any(), eq(2), eq(10), eq("createdAt"), eq("desc")))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("page", "2")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.size").value(10))
                .andExpect(jsonPath("$.meta.totalElements").value(15))
                .andExpect(jsonPath("$.meta.totalPages").value(2));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - 정렬 방향 asc")
    void listRecommendations_success_ascOrder() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(200L)
                        .summary("오래된 여행 계획")
                        .createdAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        when(destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 20, "createdAt", "asc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("order", "asc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @DisplayName("추천 목록 조회 성공 - 정렬 필드 변경")
    void listRecommendations_success_customSortField() throws Exception {
        // given
        List<DestinationRecommendationResponse.RecommendationSummary> summaries = List.of(
                DestinationRecommendationResponse.RecommendationSummary.builder()
                        .recommendationRequestId(300L)
                        .summary("정렬 테스트")
                        .createdAt(LocalDateTime.of(2024, 10, 1, 10, 0))
                        .region("default")
                        .tripPurpose("default")
                        .budgetRange("default")
                        .season("default")
                        .companionCount(1)
                        .durationDays(1)
                        .build()
        );

        Page<DestinationRecommendationResponse.RecommendationSummary> page = new PageImpl<>(
                summaries,
                PageRequest.of(0, 20),
                1
        );

        when(destinationRecommendationService.listRecommendations(
                null, null, null, null, null, 1, 20, "recommendationRequestId", "desc"))
                .thenReturn(page);

        // when & then
        mockMvc.perform(get("/v1/destination-recommendations")
                        .param("sort", "recommendationRequestId")
                        .param("order", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}

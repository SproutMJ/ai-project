
package org.mj.trip.pointrecommendation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.auth.token.JwtAuthenticationInterceptor;
import org.mj.trip.common.config.WebConfig;
import org.mj.trip.common.exception.GlobalExceptionHandler;
import org.mj.trip.common.exception.ResourceNotFoundException;
import org.mj.trip.common.service.AsyncRecommendationService;
import org.mj.trip.pointrecommendation.domain.PointRecommendation;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;
import org.mj.trip.pointrecommendation.dto.PointRecommendationDetailResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationListResponseDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestDto;
import org.mj.trip.pointrecommendation.dto.PointRecommendationRequestResponseDto;
import org.mj.trip.pointrecommendation.service.PointRecommendationService;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@DisplayName("PointRecommendationController 테스트")
@Import({WebConfig.class, GlobalExceptionHandler.class})
@WebMvcTest(PointRecommendationController.class)
class PointRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PointRecommendationService pointRecommendationService;

    @MockBean
    private JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Mock
    private AsyncRecommendationService asyncRecommendationService;

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

    @DisplayName("POST /v1/recommendations - 추천 요청 생성 성공")
    @Test
    void createRecommendation_shouldReturn201() throws Exception {
        // given
        PointRecommendationRequestDto requestDto = new PointRecommendationRequestDto("서울 여행 추천해주세요");
        String requestBody = """
                {
                    "requestText": "서울 여행 추천해주세요"
                }
                """;

        doNothing().when(asyncRecommendationService)
                .processRecommendationInBackground(anyLong(), anyString(), anyLong());

        when(pointRecommendationService.createRecommendation(anyLong(), any(PointRecommendationRequestDto.class)))
                .thenReturn(1L);

        // when & then
        mockMvc.perform(post("/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        verify(pointRecommendationService, times(1))
                .createRecommendation(eq(1L), any(PointRecommendationRequestDto.class));
    }

    @DisplayName("POST /v1/recommendations - requestText 누락 시 400 Bad Request")
    @Test
    void createRecommendation_shouldReturn400WhenRequestTextMissing() throws Exception {
        // given
        String requestBody = """
                {
                }
                """;

        // when & then
        mockMvc.perform(post("/v1/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(pointRecommendationService);
    }

    @DisplayName("GET /v1/recommendations - 추천 목록 조회 성공")
    @Test
    void listRecommendations_shouldReturnPaginatedRecommendations() throws Exception {
        // given

        long userId = 1L;
        PointRecommendationRequest dummyRequest = PointRecommendationRequest.builder()
                .id(1L)
                .userId(userId)
                .requestText("서울 여행")
                .build();

        // 참고: DTO 응답 스펙이 Request 정보만 담도록 변경되어
        // PointRecommendation 객체는 이 테스트에서 필수가 아닐 수 있지만 기존 맥락 유지를 위해 남겨둡니다.
        PointRecommendation dummyRecommendation = PointRecommendation.builder()
                .request(dummyRequest)
                .userId(userId)
                .name("경복궁")
                .recommendationScore(95.5)
                .shortComment("서울의 대표적인 궁궐")
                .type("관광지")
                .region("서울 종로구")
                .build();

        // 1. Meta 객체 생성
        PointRecommendationListResponseDto.Meta meta =
                new PointRecommendationListResponseDto.Meta(1, 10, 1, 1);

        // 2. 새로운 스펙에 맞춘 Item DTO 생성 (RecommendationSummary -> PointRecommendationRequestResponseDto 대체)
        PointRecommendationRequestResponseDto requestDto =
                new PointRecommendationRequestResponseDto(
                        dummyRequest.getId(),
                        dummyRequest.getRequestText()
                );

        // 3. 최종 Response DTO 생성
        PointRecommendationListResponseDto response =
                new PointRecommendationListResponseDto(List.of(requestDto), meta);

        when(pointRecommendationService.recommendationRequests(0, 10, "recommendationScore", "desc", userId))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/v1/recommendations")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "recommendationScore")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                // 4. jsonPath 검증 수정 (name, score 대신 id와 requestText 검증)
                .andExpect(jsonPath("$.recommendations[0].id").value(1L))
                .andExpect(jsonPath("$.recommendations[0].requestText").value("서울 여행"))
                .andExpect(jsonPath("$.meta.totalElements").value(1));

        verify(pointRecommendationService, times(1))
                .recommendationRequests(0, 10, "recommendationScore", "desc", userId);
    }

    @DisplayName("GET /v1/recommendations/{requestId} - 추천 상세 조회 성공")
    @Test
    void getRecommendationDetail_shouldReturnDetail() throws Exception {
        // given
        PointRecommendationRequest dummyRequest = PointRecommendationRequest.builder()
                .id(1L)
                .userId(1L)
                .requestText("서울 여행 추천")
                .build();

        PointRecommendation dummyRecommendation = PointRecommendation.builder()
                .request(dummyRequest)
                .userId(1L)
                .name("경복궁")
                .recommendationScore(95.5)
                .shortComment("서울의 대표적인 궁궐")
                .type("관광지")
                .region("서울 종로구")
                .build();

        PointRecommendationDetailResponseDto.RequestInfo requestInfo =
                new PointRecommendationDetailResponseDto.RequestInfo(
                        1L, 1L, "서울 여행 추천", LocalDateTime.now()
                );
        PointRecommendationDetailResponseDto.RecommendationInfo recommendationInfo =
                new PointRecommendationDetailResponseDto.RecommendationInfo(
                        dummyRecommendation.getId(),
                        dummyRecommendation.getName(),
                        dummyRecommendation.getShortComment(),
                        dummyRecommendation.getType(),
                        dummyRecommendation.getRegion(),
                        dummyRecommendation.getKeyword(),
                        dummyRecommendation.getTheme(),
                        dummyRecommendation.getBudget(),
                        dummyRecommendation.getRequiredTime(),
                        dummyRecommendation.getHowToGo(),
                        dummyRecommendation.getRecommendedPartySize(),
                        dummyRecommendation.getWeather(),
                        dummyRecommendation.getLanguage(),
                        dummyRecommendation.getDisadvantage(),
                        dummyRecommendation.getDescription(),
                        dummyRecommendation.getRecommendationScore()
                );
        PointRecommendationDetailResponseDto response =
                new PointRecommendationDetailResponseDto(requestInfo, List.of(recommendationInfo));

        when(pointRecommendationService.getRecommendationDetail(1L)).thenReturn(response);

        // when & then
        mockMvc.perform(get("/v1/recommendations/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestInfo.requestText").value("서울 여행 추천"))
                .andExpect(jsonPath("$.recommendations[0].name").value("경복궁"))
                .andExpect(jsonPath("$.recommendations[0].recommendationScore").value(95.5));

        verify(pointRecommendationService, times(1)).getRecommendationDetail(1L);
    }

    @DisplayName("GET /v1/recommendations/{requestId} - 존재하지 않는 요청 ID로 조회 시 404")
    @Test
    void getRecommendationDetail_shouldReturn404WhenNotFound() throws Exception {
        // given
        when(pointRecommendationService.getRecommendationDetail(999L))
                .thenThrow(new org.mj.trip.common.exception.ResourceNotFoundException("RecommendationRequest not found: 999"));

        // when & then
        mockMvc.perform(get("/v1/recommendations/999"))
                .andExpect(status().isNotFound());

        verify(pointRecommendationService, times(1)).getRecommendationDetail(999L);
    }

    @DisplayName("DELETE /v1/recommendations/{requestId} - 추천 요청 삭제 성공")
    @Test
    void deleteRecommendationRequest_shouldReturn204() throws Exception {
        // given
        doNothing().when(pointRecommendationService)
                .deleteRecommendationRequest(1L);

        // when & then
        mockMvc.perform(delete("/v1/recommendations/1"))
                .andExpect(status().isNoContent());

        verify(pointRecommendationService, times(1)).deleteRecommendationRequest(1L);
    }

    @DisplayName("DELETE /v1/recommendations/{requestId} - 존재하지 않는 요청 ID 삭제 시 404")
    @Test
    void deleteRecommendationRequest_shouldReturn404WhenNotFound() throws Exception {
        // given
        doThrow(new ResourceNotFoundException("RecommendationRequest not found: 999"))
                .when(pointRecommendationService)
                .deleteRecommendationRequest(999L);

        // when & then
        mockMvc.perform(delete("/v1/recommendations/999"))
                .andExpect(status().isNotFound());

        verify(pointRecommendationService, times(1)).deleteRecommendationRequest(999L);
    }
}

package org.mj.trip.destination.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DestinationRecommendationResponse DTO 테스트")
class DestinationRecommendationResponseTest {

    @Test
    @DisplayName("추천 응답 DTO 생성 성공 - 기본 케이스")
    void createResponse_success_basic() {
        // given
        Long recommendationRequestId = 1L;
        LocalDateTime createdAt = LocalDateTime.of(2023, 10, 1, 12, 0, 0);

        DestinationRecommendationResponse.Reason reason1 = DestinationRecommendationResponse.Reason.builder()
                .type("BUDGET_MATCH")
                .text("예산 범위 내에서 구성 가능합니다.")
                .build();

        DestinationRecommendationResponse.Reason reason2 = DestinationRecommendationResponse.Reason.builder()
                .type("SEASON_MATCH")
                .text("여름에 적합합니다.")
                .build();

        DestinationRecommendationResponse.Recommendation recommendation = DestinationRecommendationResponse.Recommendation.builder()
                .recommendationId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄는 쇼핑을 즐기기 좋습니다.")
                .reasons(List.of(reason1, reason2))
                .build();

        // when
        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(recommendationRequestId)
                .recommendations(List.of(recommendation))
                .createdAt(createdAt)
                .build();

        // then
        assertNotNull(response);
        assertEquals(recommendationRequestId, response.getRecommendationRequestId());
        assertEquals(1, response.getRecommendations().size());
        assertEquals(createdAt.toString(), response.getCreatedAt());
        assertEquals("도쿄", response.getRecommendations().get(0).getDestinationName());
        assertEquals(95.5, response.getRecommendations().get(0).getScore());
        assertEquals(1, response.getRecommendations().get(0).getRankOrder());
        assertEquals("도쿄는 쇼핑을 즐기기 좋습니다.", response.getRecommendations().get(0).getReasonSummary());
        assertEquals(2, response.getRecommendations().get(0).getReasons().size());
        assertEquals("BUDGET_MATCH", response.getRecommendations().get(0).getReasons().get(0).getType());
        assertEquals("예산 범위 내에서 구성 가능합니다.", response.getRecommendations().get(0).getReasons().get(0).getText());
    }

    @Test
    @DisplayName("추천 응답 DTO 생성 성공 - 여러 추천 포함")
    void createResponse_success_multipleRecommendations() {
        // given
        List<DestinationRecommendationResponse.Recommendation> recommendations = List.of(
                DestinationRecommendationResponse.Recommendation.builder()
                        .recommendationId(1L)
                        .destinationId(100L)
                        .destinationName("도쿄")
                        .score(95.5)
                        .rankOrder(1)
                        .reasonSummary("도쿄 추천")
                        .build(),
                DestinationRecommendationResponse.Recommendation.builder()
                        .recommendationId(2L)
                        .destinationId(101L)
                        .destinationName("오사카")
                        .score(90.0)
                        .rankOrder(2)
                        .reasonSummary("오사카 추천")
                        .build(),
                DestinationRecommendationResponse.Recommendation.builder()
                        .recommendationId(3L)
                        .destinationId(102L)
                        .destinationName("교토")
                        .score(85.0)
                        .rankOrder(3)
                        .reasonSummary("교토 추천")
                        .build()
        );

        // when
        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(1L)
                .recommendations(recommendations)
                .createdAt(LocalDateTime.now())
                .build();

        // then
        assertNotNull(response);
        assertEquals(3, response.getRecommendations().size());
        assertEquals("도쿄", response.getRecommendations().get(0).getDestinationName());
        assertEquals("오사카", response.getRecommendations().get(1).getDestinationName());
        assertEquals("교토", response.getRecommendations().get(2).getDestinationName());
    }

    @Test
    @DisplayName("추천 응답 DTO 생성 성공 - 이유 없음")
    void createResponse_success_noReasons() {
        // given
        DestinationRecommendationResponse.Recommendation recommendation = DestinationRecommendationResponse.Recommendation.builder()
                .recommendationId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄 추천")
                .reasons(List.of())
                .build();

        // when
        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(1L)
                .recommendations(List.of(recommendation))
                .createdAt(LocalDateTime.now())
                .build();

        // then
        assertNotNull(response);
        assertEquals(1, response.getRecommendations().size());
        assertTrue(response.getRecommendations().get(0).getReasons().isEmpty());
    }

    @Test
    @DisplayName("추천 응답 DTO 생성 성공 - 이유 null 허용 (DTO는 null 저장)")
    void createResponse_success_reasonsNull() {
        // given
        DestinationRecommendationResponse.Recommendation recommendation = DestinationRecommendationResponse.Recommendation.builder()
                .recommendationId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄 추천")
                .build();

        // when
        DestinationRecommendationResponse response = DestinationRecommendationResponse.builder()
                .recommendationRequestId(1L)
                .recommendations(List.of(recommendation))
                .createdAt(LocalDateTime.now())
                .build();

        // then
        assertNotNull(response);
        assertNull(response.getRecommendations().get(0).getReasons());
    }
}

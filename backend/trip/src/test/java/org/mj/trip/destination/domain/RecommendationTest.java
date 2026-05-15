package org.mj.trip.destination.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Recommendation Entity 테스트")
class RecommendationTest {

    @Test
    @DisplayName("추천 엔티티 생성 성공")
    void createRecommendation_success() {
        // given & when
        Recommendation recommendation = Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄는 쇼핑을 즐기기 좋고, 5일 일정에 적합합니다.")
                .build();

        // then
        assertEquals(1L, recommendation.getRecommendationRequestId());
        assertEquals(100L, recommendation.getDestinationId());
        assertEquals("도쿄", recommendation.getDestinationName());
        assertEquals(95.5, recommendation.getScore());
        assertEquals(1, recommendation.getRankOrder());
        assertEquals("도쿄는 쇼핑을 즐기기 좋고, 5일 일정에 적합합니다.", recommendation.getReasonSummary());
    }

    @Test
    @DisplayName("추천 엔티티 필수 필드 검증")
    void createRecommendation_requiredFields() {
        // given & when
        Recommendation recommendation = Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("부산")
                .score(88.0)
                .rankOrder(2)
                .reasonSummary("부산은 휴식과 2명의 동반자와 3일 일정에 적합합니다.")
                .build();

        // then - 모든 필수 필드가 null이 아님을 검증
        assertNotNull(recommendation.getRecommendationRequestId());
        assertNotNull(recommendation.getDestinationId());
        assertNotNull(recommendation.getDestinationName());
        assertNotNull(recommendation.getScore());
        assertNotNull(recommendation.getRankOrder());
        assertNotNull(recommendation.getReasonSummary());
    }

    @Test
    @DisplayName("추천 엔티티 다양한 점수 범위 테스트")
    void createRecommendation_scoreRange() {
        // given & when
        Recommendation recommendation = Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(200L)
                .destinationName("방콕")
                .score(80.0)
                .rankOrder(5)
                .reasonSummary("방콕은 휴식과 3명의 동반자와 7일 일정에 적합합니다.")
                .build();

        // then
        assertEquals(80.0, recommendation.getScore());
        assertEquals(5, recommendation.getRankOrder());
    }
}

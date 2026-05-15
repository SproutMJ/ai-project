package org.mj.trip.destination.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecommendationReason Entity 테스트")
class RecommendationReasonTest {

    @Test
    @DisplayName("추천 이유 엔티티 생성 성공")
    void createRecommendationReason_success() {
        // given & when
        RecommendationReason reason = RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.BUDGET_MATCH)
                .text("예산 범위 (50-100만) 내에서 충분히 구성 가능합니다.")
                .build();

        // then
        assertEquals(1L, reason.getRecommendationId());
        assertEquals(ReasonType.BUDGET_MATCH, reason.getType());
        assertEquals("예산 범위 (50-100만) 내에서 충분히 구성 가능합니다.", reason.getText());
    }

    @Test
    @DisplayName("추천 이유 엔티티 다양한 ReasonType 테스트")
    void createRecommendationReason_variousTypes() {
        // given & when
        RecommendationReason reason = RecommendationReason.builder()
                .recommendationId(2L)
                .type(ReasonType.SEASON_MATCH)
                .text("여름 계절에 적합한 활동과 경관을 즐길 수 있습니다.")
                .build();

        // then
        assertEquals(ReasonType.SEASON_MATCH, reason.getType());
        assertEquals(2L, reason.getRecommendationId());
    }

    @Test
    @DisplayName("추천 이유 엔티티 필수 필드 검증")
    void createRecommendationReason_requiredFields() {
        // given & when
        RecommendationReason reason = RecommendationReason.builder()
                .recommendationId(3L)
                .type(ReasonType.DURATION_MATCH)
                .text("5일 일정으로 충분히 즐길 수 있는 코스입니다.")
                .build();

        // then - 모든 필수 필드가 null이 아님을 검증
        assertNotNull(reason.getRecommendationId());
        assertNotNull(reason.getType());
        assertNotNull(reason.getText());
    }
}

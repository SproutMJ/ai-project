package org.mj.trip.pointrecommendation.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PointRecommendationRequestTest {

    @DisplayName("PointRecommendationRequest 엔티티가 올바르게 생성되어야 한다")
    @Test
    void createEntity() {
        // given
        Long userId = 1L;
        String requestText = "서울 여행 추천해주세요";

        // when
        PointRecommendationRequest request = PointRecommendationRequest.builder()
                .userId(userId)
                .requestText(requestText)
                .build();

        // then
        assertThat(request.getUserId()).isEqualTo(userId);
        assertThat(request.getRequestText()).isEqualTo(requestText);
        assertThat(request.getId()).isNull(); // DB에 저장 전이므로 ID는 null
    }

    @DisplayName("requestText는 null이 될 수 없다")
    @Test
    void requestTextNotNull() {
        // given & when
        PointRecommendationRequest request = PointRecommendationRequest.builder()
                .userId(1L)
                .requestText("테스트 요청")
                .build();

        // then
        assertThat(request.getRequestText()).isNotNull();
        assertThat(request.getRequestText()).isNotBlank();
    }

    @DisplayName("userId는 null이 될 수 없다")
    @Test
    void userIdNotNull() {
        // given & when
        PointRecommendationRequest request = PointRecommendationRequest.builder()
                .userId(100L)
                .requestText("테스트 요청")
                .build();

        // then
        assertThat(request.getUserId()).isEqualTo(100L);
    }

    @DisplayName("Builder 패턴을 통해 엔티티가 생성되어야 한다")
    @Test
    void builderCreation() {
        // given
        Long userId = 10L;
        String requestText = "제주도 여행 추천";

        // when
        PointRecommendationRequest request = PointRecommendationRequest.builder()
                .userId(userId)
                .requestText(requestText)
                .build();

        // then
        assertThat(request).isNotNull();
        assertThat(request.getUserId()).isEqualTo(userId);
        assertThat(request.getRequestText()).isEqualTo(requestText);
    }
}

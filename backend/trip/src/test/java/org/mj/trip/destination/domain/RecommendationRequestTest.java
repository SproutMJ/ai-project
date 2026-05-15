package org.mj.trip.destination.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecommendationRequest Entity 테스트")
class RecommendationRequestTest {

    @Test
    @DisplayName("추천 요청 엔티티 생성 성공")
    void createRecommendationRequest_success() {
        // given & when
        RecommendationRequest request = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("50-100만")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .build();

        // then
        assertEquals(1L, request.getMemberId());
        assertEquals("휴식", request.getTripPurpose());
        assertEquals("50-100만", request.getBudgetRange());
        assertEquals("일본", request.getRegion());
        assertEquals("여름", request.getSeason());
        assertEquals(2, request.getCompanionCount());
        assertEquals(5, request.getDurationDays());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    @DisplayName("추천 요청 엔티티 생성 시 createdAt 자동 설정")
    void createRecommendationRequest_createdAtAutoSet() {
        // given
        LocalDateTime before = LocalDateTime.now();

        // when
        RecommendationRequest request = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("쇼핑")
                .budgetRange("100-200만")
                .region("태국")
                .season("겨울")
                .companionCount(4)
                .durationDays(7)
                .build();

        LocalDateTime after = LocalDateTime.now();

        // then
        assertNotNull(request.getCreatedAt());
        assertTrue(request.getCreatedAt().isAfter(before) || request.getCreatedAt().isEqual(before));
        assertTrue(request.getCreatedAt().isBefore(after) || request.getCreatedAt().isEqual(after));
    }

    @Test
    @DisplayName("추천 요청 엔티티 필수 필드 검증")
    void createRecommendationRequest_requiredFields() {
        // given & when
        RecommendationRequest request = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("여행")
                .budgetRange("200만 이상")
                .region("베트남")
                .season("봄")
                .companionCount(1)
                .durationDays(3)
                .build();

        // then - 모든 필수 필드가 null이 아님을 검증
        assertNotNull(request.getMemberId());
        assertNotNull(request.getTripPurpose());
        assertNotNull(request.getBudgetRange());
        assertNotNull(request.getRegion());
        assertNotNull(request.getSeason());
        assertNotNull(request.getCompanionCount());
        assertNotNull(request.getDurationDays());
    }
}

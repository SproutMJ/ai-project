package org.mj.trip.destination.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.destination.domain.RecommendationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecommendationRequestRepository 테스트")
@DataJpaTest
class RecommendationRequestRepositoryTest {

    @Autowired
    private RecommendationRequestRepository recommendationRequestRepository;

    @Test
    @DisplayName("추천 요청 저장 및 ID 조회 성공")
    void saveAndFindById_success() {
        // given
        RecommendationRequest request = RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("50-100만")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .summary("휴식 여행 요청")
                .build();

        // when
        RecommendationRequest saved = recommendationRequestRepository.save(request);

        // then
        assertNotNull(saved.getRecommendationRequestId());
        assertEquals("휴식", saved.getTripPurpose());
        assertEquals("일본", saved.getRegion());
    }

    @Test
    @DisplayName("멤버 ID로 추천 요청 목록 조회 - 성공")
    void findByMemberId_success() {
        // given
        recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("50-100만")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .summary("휴식 여행 요청")
                .build());
        recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("쇼핑")
                .budgetRange("100-200만")
                .region("태국")
                .season("겨울")
                .companionCount(4)
                .durationDays(7)
                .summary("쇼핑 여행 요청")
                .build());
        recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(2L)
                .tripPurpose("관광")
                .budgetRange("200만 이상")
                .region("베트남")
                .season("봄")
                .companionCount(1)
                .durationDays(3)
                .summary("관광 여행 요청")
                .build());

        // when
        List<RecommendationRequest> requests = recommendationRequestRepository.findByMemberIdOrderByCreatedAtDesc(1L);

        // then
        assertEquals(2, requests.size());
    }

    @Test
    @DisplayName("멤버 ID로 추천 요청 목록 조회 - 데이터 없음")
    void findByMemberId_empty() {
        // when
        List<RecommendationRequest> requests = recommendationRequestRepository.findByMemberIdOrderByCreatedAtDesc(999L);

        // then
        assertTrue(requests.isEmpty());
    }

    @Test
    @DisplayName("추천 요청 삭제 - 성공")
    void delete_success() {
        // given
        RecommendationRequest saved = recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("휴식")
                .budgetRange("50-100만")
                .region("일본")
                .season("여름")
                .companionCount(2)
                .durationDays(5)
                .summary("휴식 여행 요청")
                .build());
        Long id = saved.getRecommendationRequestId();

        // when
        recommendationRequestRepository.deleteById(id);

        // then
        Optional<RecommendationRequest> deleted = recommendationRequestRepository.findById(id);
        assertFalse(deleted.isPresent());
    }
}

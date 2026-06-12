package org.mj.trip.destination.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.destination.domain.Recommendation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecommendationRepository 테스트")
@DataJpaTest
class RecommendationRepositoryTest {

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Test
    @DisplayName("추천 저장 및 ID 조회 성공")
    void saveAndFindById_success() {
        // given
        Recommendation recommendation = Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄는 쇼핑을 즐기기 좋습니다.")
                .build();

        // when
        Recommendation saved = recommendationRepository.save(recommendation);

        // then
        assertNotNull(saved.getId());
        assertEquals("도쿄", saved.getDestinationName());
        assertEquals(95.5, saved.getScore());
    }

    @Test
    @DisplayName("추천 요청 ID로 추천 목록 조회 - 성공")
    void findByRecommendationRequestId_success() {
        // given
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(101L)
                .destinationName("오사카")
                .score(90.0)
                .rankOrder(2)
                .reasonSummary("오사카 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(2L)
                .destinationId(200L)
                .destinationName("방콕")
                .score(88.0)
                .rankOrder(1)
                .reasonSummary("방콕 추천")
                .build());

        // when
        List<Recommendation> recommendations = recommendationRepository.findByRecommendationRequestIdOrderByRankOrderAsc(1L);

        // then
        assertEquals(2, recommendations.size());
        assertEquals("도쿄", recommendations.get(0).getDestinationName());
        assertEquals("오사카", recommendations.get(1).getDestinationName());
    }

    @Test
    @DisplayName("추천 요청 ID로 추천 목록 조회 - 데이터 없음")
    void findByRecommendationRequestId_empty() {
        // when
        List<Recommendation> recommendations = recommendationRepository.findByRecommendationRequestIdOrderByRankOrderAsc(999L);

        // then
        assertTrue(recommendations.isEmpty());
    }

    @Test
    @DisplayName("추천 요청 ID로 추천 삭제 - 성공")
    void deleteByRecommendationRequestId_success() {
        // given
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(1L)
                .destinationId(101L)
                .destinationName("오사카")
                .score(90.0)
                .rankOrder(2)
                .reasonSummary("오사카 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(2L)
                .destinationId(200L)
                .destinationName("방콕")
                .score(88.0)
                .rankOrder(1)
                .reasonSummary("방콕 추천")
                .build());

        // when
        recommendationRepository.deleteByRecommendationRequestId(1L);

        // then
        List<Recommendation> remaining = recommendationRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals("방콕", remaining.get(0).getDestinationName());
    }
}

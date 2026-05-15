package org.mj.trip.destination.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.destination.domain.ReasonType;
import org.mj.trip.destination.domain.RecommendationReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RecommendationReasonRepository 테스트")
@DataJpaTest
class RecommendationReasonRepositoryTest {

    @Autowired
    private RecommendationReasonRepository recommendationReasonRepository;

    @Test
    @DisplayName("추천 이유 저장 및 ID 조회 성공")
    void saveAndFindById_success() {
        // given
        RecommendationReason reason = RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.BUDGET_MATCH)
                .text("예산 범위 (50-100만) 내에서 충분히 구성 가능합니다.")
                .build();

        // when
        RecommendationReason saved = recommendationReasonRepository.save(reason);

        // then
        assertNotNull(saved.getId());
        assertEquals(ReasonType.BUDGET_MATCH, saved.getType());
        assertEquals("예산 범위 (50-100만) 내에서 충분히 구성 가능합니다.", saved.getText());
    }

    @Test
    @DisplayName("추천 ID로 추천 이유 목록 조회 - 성공")
    void findByRecommendationId_success() {
        // given
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.BUDGET_MATCH)
                .text("예산 범위 내에서 구성 가능합니다.")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.SEASON_MATCH)
                .text("여름 계절에 적합한 활동을 즐길 수 있습니다.")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.DURATION_MATCH)
                .text("5일 일정으로 충분히 즐길 수 있습니다.")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(2L)
                .type(ReasonType.BUDGET_MATCH)
                .text("다른 추천의 예산 매칭")
                .build());

        // when
        List<RecommendationReason> reasons = recommendationReasonRepository.findByRecommendationId(1L);

        // then
        assertEquals(3, reasons.size());
    }

    @Test
    @DisplayName("추천 ID로 추천 이유 목록 조회 - 데이터 없음")
    void findByRecommendationId_empty() {
        // when
        List<RecommendationReason> reasons = recommendationReasonRepository.findByRecommendationId(999L);

        // then
        assertTrue(reasons.isEmpty());
    }

    @Test
    @DisplayName("추천 ID로 추천 이유 삭제 - 성공")
    void deleteByRecommendationId_success() {
        // given
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.BUDGET_MATCH)
                .text("예산 범위 내에서 구성 가능합니다.")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.SEASON_MATCH)
                .text("여름 계절에 적합한 활동을 즐길 수 있습니다.")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(2L)
                .type(ReasonType.BUDGET_MATCH)
                .text("다른 추천의 예산 매칭")
                .build());

        // when
        recommendationReasonRepository.deleteByRecommendationId(1L);

        // then
        List<RecommendationReason> remaining = recommendationReasonRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(2L, remaining.get(0).getRecommendationId());
    }

    @Test
    @DisplayName("추천 ID 목록으로 추천 이유 삭제 - 성공")
    void deleteByRecommendationIdIn_success() {
        // given
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(1L)
                .type(ReasonType.BUDGET_MATCH)
                .text("추천 1 예산 매칭")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(2L)
                .type(ReasonType.SEASON_MATCH)
                .text("추천 2 계절 매칭")
                .build());
        recommendationReasonRepository.save(RecommendationReason.builder()
                .recommendationId(3L)
                .type(ReasonType.DURATION_MATCH)
                .text("추천 3 일정 매칭")
                .build());

        // when
        recommendationReasonRepository.deleteByRecommendationIdIn(List.of(1L, 2L));

        // then
        List<RecommendationReason> remaining = recommendationReasonRepository.findAll();
        assertEquals(1, remaining.size());
        assertEquals(3L, remaining.get(0).getRecommendationId());
    }
}

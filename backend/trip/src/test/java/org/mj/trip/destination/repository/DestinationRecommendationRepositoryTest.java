package org.mj.trip.destination.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.destination.domain.Recommendation;
import org.mj.trip.destination.domain.RecommendationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DestinationRecommendationRepository 통합 테스트")
@DataJpaTest
class DestinationRecommendationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecommendationRequestRepository recommendationRequestRepository;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Test
    @DisplayName("RecommendationRequest 및 Recommendation 저장 - ID 자동 생성 및 1:N 관계 검증 성공")
    void saveRecommendationRequest_and_recommendations() {
        // given - RecommendationRequest 생성
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

        // when - RecommendationRequest 저장
        RecommendationRequest savedRequest = recommendationRequestRepository.save(request);
        entityManager.flush();

        // Recommendation 3개 생성 (연관관계 설정)
        Recommendation rec1 = Recommendation.builder()
                .recommendationRequestId(savedRequest.getRecommendationRequestId())
                .destinationId(100L)
                .destinationName("도쿄")
                .score(95.5)
                .rankOrder(1)
                .reasonSummary("도쿄는 쇼핑을 즐기기 좋습니다.")
                .build();
        Recommendation rec2 = Recommendation.builder()
                .recommendationRequestId(savedRequest.getRecommendationRequestId())
                .destinationId(101L)
                .destinationName("오사카")
                .score(90.0)
                .rankOrder(2)
                .reasonSummary("오사카는 음식이 유명합니다.")
                .build();
        Recommendation rec3 = Recommendation.builder()
                .recommendationRequestId(savedRequest.getRecommendationRequestId())
                .destinationId(102L)
                .destinationName("교토")
                .score(88.0)
                .rankOrder(3)
                .reasonSummary("교토는 역사가 풍부합니다.")
                .build();

        recommendationRepository.saveAll(List.of(rec1, rec2, rec3));
        entityManager.flush();

        // then - RecommendationRequest ID 검증
        assertThat(savedRequest.getRecommendationRequestId()).isNotNull();

        // then - Recommendation IDs 검증
        List<Recommendation> allRecommendations = recommendationRepository.findAll();
        assertThat(allRecommendations).hasSize(3);
        assertThat(rec1.getId()).isNotNull();
        assertThat(rec2.getId()).isNotNull();
        assertThat(rec3.getId()).isNotNull();

        // then - 1:N 연관관계 검증 (Recommendation의 recommendationRequestId가 Request의 ID와 일치)
        Long requestId = savedRequest.getRecommendationRequestId();
        rec1 = recommendationRepository.findById(rec1.getId()).orElseThrow();
        rec2 = recommendationRepository.findById(rec2.getId()).orElseThrow();
        rec3 = recommendationRepository.findById(rec3.getId()).orElseThrow();

        assertThat(rec1.getRecommendationRequestId()).isEqualTo(requestId);
        assertThat(rec2.getRecommendationRequestId()).isEqualTo(requestId);
        assertThat(rec3.getRecommendationRequestId()).isEqualTo(requestId);
    }

    @Test
    @DisplayName("RecommendationRequest findById 성공")
    void findRecommendationRequestById_success() {
        // given
        RecommendationRequest savedRequest = recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(1L)
                .tripPurpose("쇼핑")
                .budgetRange("100-200만")
                .region("태국")
                .season("겨울")
                .companionCount(4)
                .durationDays(7)
                .summary("쇼핑 여행 요청")
                .build());
        Long requestId = savedRequest.getRecommendationRequestId();

        // when
        Optional<RecommendationRequest> foundRequest = recommendationRequestRepository.findById(requestId);

        // then
        assertThat(foundRequest).isPresent();
        assertThat(foundRequest.get().getRecommendationRequestId()).isEqualTo(requestId);
        assertThat(foundRequest.get().getTripPurpose()).isEqualTo("쇼핑");
    }

    @Test
    @DisplayName("RecommendationRequest findById - 데이터 없음")
    void findRecommendationRequestById_notFound() {
        // when
        Optional<RecommendationRequest> foundRequest = recommendationRequestRepository.findById(999L);

        // then
        assertThat(foundRequest).isEmpty();
    }

    @Test
    @DisplayName("Recommendation by RecommendationRequest 조회 성공")
    void findByRecommendationRequest_success() {
        // given
        RecommendationRequest savedRequest = recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(2L)
                .tripPurpose("관광")
                .budgetRange("200만 이상")
                .region("일본")
                .season("봄")
                .companionCount(1)
                .durationDays(3)
                .summary("관광 여행 요청")
                .build());
        Long requestId = savedRequest.getRecommendationRequestId();

        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(requestId)
                .destinationId(200L)
                .destinationName("도쿄")
                .score(95.0)
                .rankOrder(1)
                .reasonSummary("도쿄 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(requestId)
                .destinationId(201L)
                .destinationName("오사카")
                .score(92.0)
                .rankOrder(2)
                .reasonSummary("오사카 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(requestId)
                .destinationId(202L)
                .destinationName("교토")
                .score(87.0)
                .rankOrder(3)
                .reasonSummary("교토 추천")
                .build());
        entityManager.flush();

        // when
        List<Recommendation> recommendations = recommendationRepository
                .findByRecommendationRequestIdOrderByRankOrderAsc(requestId);

        // then
        assertThat(recommendations).hasSize(3);
        assertThat(recommendations.get(0).getDestinationName()).isEqualTo("도쿄");
        assertThat(recommendations.get(1).getDestinationName()).isEqualTo("오사카");
        assertThat(recommendations.get(2).getDestinationName()).isEqualTo("교토");
        assertThat(recommendations.get(0).getRankOrder()).isEqualTo(1);
        assertThat(recommendations.get(1).getRankOrder()).isEqualTo(2);
        assertThat(recommendations.get(2).getRankOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("Recommendation by RecommendationRequest 조회 - 데이터 없음")
    void findByRecommendationRequest_empty() {
        // when
        List<Recommendation> recommendations = recommendationRepository
                .findByRecommendationRequestIdOrderByRankOrderAsc(999L);

        // then
        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("RecommendationRequest 및 관련 Recommendation 삭제 성공")
    void deleteByRecommendationRequestId_success() {
        // given
        RecommendationRequest savedRequest = recommendationRequestRepository.save(RecommendationRequest.builder()
                .memberId(3L)
                .tripPurpose("힐링")
                .budgetRange("50만 미만")
                .region("베트남")
                .season("여름")
                .companionCount(1)
                .durationDays(3)
                .summary("힐링 여행 요청")
                .build());
        Long requestId = savedRequest.getRecommendationRequestId();

        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(requestId)
                .destinationId(300L)
                .destinationName("하노이")
                .score(85.0)
                .rankOrder(1)
                .reasonSummary("하노이 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(requestId)
                .destinationId(301L)
                .destinationName("호찌민")
                .score(82.0)
                .rankOrder(2)
                .reasonSummary("호찌민 추천")
                .build());
        recommendationRepository.save(Recommendation.builder()
                .recommendationRequestId(requestId)
                .destinationId(302L)
                .destinationName("대낭")
                .score(80.0)
                .rankOrder(3)
                .reasonSummary("대낭 추천")
                .build());
        entityManager.flush();

        // when
        recommendationRepository.deleteByRecommendationRequestId(requestId);
        entityManager.flush();

        // then
        List<Recommendation> remaining = recommendationRepository
                .findByRecommendationRequestIdOrderByRankOrderAsc(requestId);
        assertThat(remaining).isEmpty();
    }
}

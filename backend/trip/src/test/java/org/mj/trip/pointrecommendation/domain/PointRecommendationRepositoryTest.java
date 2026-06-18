package org.mj.trip.pointrecommendation.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PointRecommendationRepositoryTest {
    @Autowired
    private PointRecommendationRequestRepository requestRepository;

    @Autowired
    private PointRecommendationRepository repository;

    private PointRecommendationRequest request;

    @BeforeEach
    void setUp() {
        request = PointRecommendationRequest.builder()
                .userId(1L)
                .requestText("서울 여행 추천 요청")
                .build();
    }

    @Test
    void findByRequestOrderByRecommendationScoreDescWithPageable() {
        // given
        PointRecommendationRequest savedRequest = requestRepository.save(request);
        // request가 저장되어야 ID를 가짐. PointRecommendationRequestRepository 사용 필요.
        // 여기서는 가정.

        for (int i = 0; i < 5; i++) {
            repository.save(PointRecommendation.builder()
                    .request(request)
                    .userId(1L)
                    .name("추천 장소 " + i)
                    .recommendationScore((double) (i + 1) / 10.0)
                    .shortComment("설명 " + i)
                    .type("장소")
                    .region("서울")
                    .build());
        }

        Pageable pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "recommendationScore"));

        // when
        Page<PointRecommendation> page = repository.findByRequestOrderByRecommendationScoreDesc(request, pageable);

        // then
        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent().get(0).getRecommendationScore()).isGreaterThan(page.getContent().get(1).getRecommendationScore());
        assertThat(page.getContent().get(1).getRecommendationScore()).isGreaterThan(page.getContent().get(2).getRecommendationScore());
    }

    @DisplayName("요청 ID에 해당하는 모든 추천을 삭제해야 한다")
    @Test
    void deleteByRequestId() {
        // given
        PointRecommendationRequest savedRequest = requestRepository.save(request);

        repository.save(PointRecommendation.builder()
                .request(request)
                .userId(1L)
                .name("추천 1")
                .recommendationScore(0.9)
                .shortComment("설명1")
                .type("장소")
                .region("서울")
                .build());

        repository.save(PointRecommendation.builder()
                .request(request)
                .userId(1L)
                .name("추천 2")
                .recommendationScore(0.8)
                .shortComment("설명2")
                .type("장소")
                .region("서울")
                .build());

        // when
        repository.deleteByRequestId(savedRequest.getId());

        // then
        List<PointRecommendation> results = repository.findByRequestOrderByRecommendationScoreDesc(request);
        assertThat(results).isEmpty();
    }

    @DisplayName("존재하지 않는 요청 ID로 삭제 시 예외가 발생하지 않아야 한다")
    @Test
    void deleteByRequestIdNotFound() {
        // when & then
        // 존재하지 않는 ID로 삭제해도 예외가 발생하지 않아야 함
        repository.deleteByRequestId(999L);
        assertThat(true).isTrue();
    }
}

package org.mj.trip.pointrecommendation.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.pointrecommendation.domain.PointRecommendationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
    org.mj.trip.pointrecommendation.domain.PointRecommendationRequest.class,
    org.mj.trip.pointrecommendation.domain.PointRecommendation.class
})
class PointRecommendationRequestRepositoryTest {

    @Autowired
    private PointRecommendationRequestRepository repository;

    private PointRecommendationRequest request;

    @BeforeEach
    void setUp() {
        request = PointRecommendationRequest.builder()
                .userId(1L)
                .requestText("서울 여행 추천 요청")
                .build();
    }

    @DisplayName("엔티티를 저장하고 ID를 확인해야 한다")
    @Test
    void saveAndFindById() {
        // when
        PointRecommendationRequest saved = repository.save(request);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getRequestText()).isEqualTo("서울 여행 추천 요청");

        Optional<PointRecommendationRequest> found = repository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getRequestText()).isEqualTo("서울 여행 추천 요청");
    }

    @DisplayName("저장된 엔티티를 삭제해야 한다")
    @Test
    void delete() {
        // given
        PointRecommendationRequest saved = repository.save(request);
        Long id = saved.getId();

        // when
        repository.deleteById(id);

        // then
        Optional<PointRecommendationRequest> found = repository.findById(id);
        assertThat(found).isEmpty();
    }

    @DisplayName("여러 엔티티를 저장 후 전체 조회해야 한다")
    @Test
    void findAll() {
        // given
        PointRecommendationRequest request2 = PointRecommendationRequest.builder()
                .userId(2L)
                .requestText("제주도 여행 추천 요청")
                .build();
        repository.save(request);
        repository.save(request2);

        // when
        List<PointRecommendationRequest> all = repository.findAll();

        // then
        assertThat(all).hasSize(2);
        assertThat(all).extracting("userId")
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @DisplayName("엔티티를 업데이트해야 한다")
    @Test
    void update() {
        // given
        PointRecommendationRequest saved = repository.save(request);
        Long id = saved.getId();

        // when - requestText 변경 후 다시 저장
        PointRecommendationRequest updated = repository.findById(id).orElseThrow();
        // Note: JPA는 dirty checking으로 변경 감지
        // 그러나 @NoArgsConstructor(access = PROTECTED)이므로 직접 필드 변경이 불가
        // 테스트에서는 새 객체로 덮어쓰는 방식으로 검증

        PointRecommendationRequest newRequest = PointRecommendationRequest.builder()
                .id(id)
                .userId(1L)
                .requestText("변경된 요청")
                .build();

        // JPA는 ID가 있으면 merge(업데이트) 동작
        PointRecommendationRequest merged = repository.save(newRequest);

        // then
        Optional<PointRecommendationRequest> found = repository.findById(id);
        assertThat(found).isPresent();
        assertThat(found.get().getRequestText()).isEqualTo("변경된 요청");
    }

    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional을 반환해야 한다")
    @Test
    void findByIdNotFound() {
        // when
        Optional<PointRecommendationRequest> found = repository.findById(999L);

        // then
        assertThat(found).isEmpty();
    }

    @DisplayName("엔티티 카운트를 반환해야 한다")
    @Test
    void count() {
        // given
        repository.save(PointRecommendationRequest.builder()
                .userId(1L)
                .requestText("요청1")
                .build());
        repository.save(PointRecommendationRequest.builder()
                .userId(2L)
                .requestText("요청2")
                .build());

        // when
        long count = repository.count();

        // then
        assertThat(count).isEqualTo(2);
    }
}

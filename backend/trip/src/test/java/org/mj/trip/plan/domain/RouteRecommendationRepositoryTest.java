package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteRecommendationRepositoryTest {

    @Autowired
    private RouteRecommendationRepository routeRecommendationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final LocalDate START_DATE = LocalDate.of(2026, 7, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 7, 3);

    @Test
    @DisplayName("RouteRecommendation 저장 및 findById로 조회")
    void saveAndFindById() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();

        // when
        RouteRecommendation saved = routeRecommendationRepository.save(routeRecommendation);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<RouteRecommendation> found = routeRecommendationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        RouteRecommendation result = found.get();
        assertThat(result.getRequestId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("서울 여행 추천");
        assertThat(result.getRecommendationScore()).isEqualTo(4.8);
        assertThat(result.getShortComment()).isEqualTo("서울의 명소를 효율적으로 돌아보는 코스");
        assertThat(result.getBudget()).isEqualTo("50000");
        assertThat(result.getRegion()).isEqualTo("서울특별시");
    }

    @Test
    @DisplayName("userId로 RouteRecommendation 목록 조회")
    void findByUserId() {
        // given
        RouteRecommendation route1 = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("여행 추천 1")
                .recommendationScore(4.5)
                .shortComment("설명 1")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(route1);

        RouteRecommendation route2 = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("여행 추천 2")
                .recommendationScore(4.2)
                .shortComment("설명 2")
                .region("경기도")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(route2);

        RouteRecommendation route3 = RouteRecommendation.builder()
                .requestId(2L)
                .userId(2L)
                .name("다른 사용자 여행")
                .recommendationScore(4.0)
                .shortComment("다른 사용자")
                .region("부산광역시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(route3);

        entityManager.flush();
        entityManager.clear();

        // when
        List<RouteRecommendation> results = routeRecommendationRepository.findByUserId(1L);

        // then
        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactlyInAnyOrder("여행 추천 1", "여행 추천 2");
    }

    @Test
    @DisplayName("userId로 조회 시 해당 사용자가 없으면 빈 리스트를 반환한다")
    void findByUserId_emptyResult() {
        // given
        Long nonExistentUserId = 999L;

        // when
        List<RouteRecommendation> results = routeRecommendationRepository.findByUserId(nonExistentUserId);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("id와 userId로 RouteRecommendation 조회")
    void findByIdAndUserId() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(routeRecommendation);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<RouteRecommendation> found = routeRecommendationRepository
                .findByIdAndUserId(routeRecommendation.getId(), routeRecommendation.getUserId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(routeRecommendation.getId());
        assertThat(found.get().getName()).isEqualTo("서울 여행 추천");
    }

    @Test
    @DisplayName("id는 맞지만 userId가 다르면 null을 반환한다")
    void findByIdAndUserId_wrongUserId() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(routeRecommendation);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<RouteRecommendation> found = routeRecommendationRepository
                .findByIdAndUserId(routeRecommendation.getId(), 999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findWithItemsById - daySchedules와 scheduleItems가 FETCH되어 조회된다")
    void findWithItemsById() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .name("1일차")
                .routeRecommendation(routeRecommendation)
                .build();
        entityManager.persist(daySchedule);
        entityManager.flush();
        entityManager.clear();

        // when
        Optional<RouteRecommendation> found = routeRecommendationRepository
                .findWithItemsById(routeRecommendation.getId());

        // then
        assertThat(found).isPresent();
        RouteRecommendation result = found.get();
        assertThat(result.getName()).isEqualTo("서울 여행 추천");
        assertThat(result.getDaySchedules()).isNotEmpty();
        assertThat(result.getDaySchedules()).hasSize(1);
        assertThat(result.getDaySchedules().get(0).getDayNumber()).isEqualTo(1);
    }

    @Test
    @DisplayName("findWithItemsById - 존재하지 않는 id로 조회 시 empty를 반환한다")
    void findWithItemsById_nonExistentId() {
        // given
        Long nonExistentId = 999L;

        // when
        Optional<RouteRecommendation> found = routeRecommendationRepository
                .findWithItemsById(nonExistentId);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("deleteById로 RouteRecommendation 삭제")
    void deleteById() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("삭제 테스트 여행")
                .recommendationScore(4.0)
                .shortComment("삭제 테스트")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        RouteRecommendation saved = routeRecommendationRepository.save(routeRecommendation);
        Long id = saved.getId();
        entityManager.flush();
        entityManager.clear();

        // when
        routeRecommendationRepository.deleteById(id);
        entityManager.flush();
        entityManager.clear();

        // then
        Optional<RouteRecommendation> found = routeRecommendationRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName(" orphanRemoval 설정으로 daySchedules가 함께 삭제된다")
    void orphanRemoval_daySchedulesDeleted() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("오프런 삭제 테스트")
                .recommendationScore(4.5)
                .shortComment("orphanRemoval 테스트")
                .region("서울특별시")
                .startDate(START_DATE)
                .endDate(END_DATE)
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .name("1일차")
                .routeRecommendation(routeRecommendation)
                .build();
        entityManager.persist(daySchedule);
        entityManager.flush();
        entityManager.clear();

        Long dayScheduleId = daySchedule.getId();

        // when
        routeRecommendationRepository.deleteById(routeRecommendation.getId());
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(routeRecommendationRepository.findById(routeRecommendation.getId())).isEmpty();
        assertThat(entityManager.find(RouteDaySchedule.class, dayScheduleId)).isNull();
    }
}

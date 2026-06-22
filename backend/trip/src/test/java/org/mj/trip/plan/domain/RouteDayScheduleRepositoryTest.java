package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteDayScheduleRepositoryTest {

    @Autowired
    private RouteDayScheduleRepository routeDayScheduleRepository;

    @Autowired
    private RouteRecommendationRepository routeRecommendationRepository;

    @Test
    @DisplayName("routeRecommendationId로 조회 시 dayNumber 오름차순 정렬되어 반환된다")
    void findByRouteRecommendationIdOrderByDayNumberAsc() {
        // given
        // 수정: recommendationScore, shortComment, region 등 nullable=false인 필드 값 설정
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .startDate(LocalDate.now())
                .endDate(LocalDate.now())
                .budget(String.valueOf(BigDecimal.valueOf(1000000)))
                .requestId(1L)
                .userId(1L)
                .name("테스트 여행")
                .recommendationScore(4.5)
                .shortComment("테스트 설명")
                .region("서울특별시")
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        RouteDaySchedule daySchedule1 = RouteDaySchedule.builder()
                .dayNumber(1)
                .name("1일차")
                .routeRecommendation(routeRecommendation)
                .build();
        routeDayScheduleRepository.save(daySchedule1);

        RouteDaySchedule daySchedule2 = RouteDaySchedule.builder()
                .dayNumber(3)
                .name("2일차")
                .routeRecommendation(routeRecommendation)
                .build();
        routeDayScheduleRepository.save(daySchedule2);

        RouteDaySchedule daySchedule3 = RouteDaySchedule.builder()
                .dayNumber(2)
                .name("3일차")
                .routeRecommendation(routeRecommendation)
                .build();
        routeDayScheduleRepository.save(daySchedule3);

        // when
        List<RouteDaySchedule> result = routeDayScheduleRepository
                .findByRouteRecommendationIdOrderByDayNumberAsc(routeRecommendation.getId());

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDayNumber()).isEqualTo(1);
        assertThat(result.get(1).getDayNumber()).isEqualTo(2);
        assertThat(result.get(2).getDayNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("routeRecommendationId에 해당하는 daySchedule이 없으면 빈 리스트를 반환한다")
    void findByRouteRecommendationIdOrderByDayNumberAsc_emptyResult() {
        // given
        Long nonExistentId = 999L;

        // when
        List<RouteDaySchedule> result = routeDayScheduleRepository
                .findByRouteRecommendationIdOrderByDayNumberAsc(nonExistentId);

        // then
        assertThat(result).isEmpty();
    }
}

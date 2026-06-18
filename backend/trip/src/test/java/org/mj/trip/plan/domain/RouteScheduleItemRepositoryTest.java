
package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RouteScheduleItemRepositoryTest {

    @Autowired
    private RouteRecommendationRepository routeRecommendationRepository;

    @Autowired
    private RouteScheduleItemRepository routeScheduleItemRepository;

    @Autowired
    private RouteDayScheduleRepository routeDayScheduleRepository;

    @Test
    @DisplayName("RouteScheduleItem - NODE 타입 저장 및 조회")
    void saveAndFindNodeItem() {
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        // given
        RouteDaySchedule daySchedule = routeDayScheduleRepository.save(
                RouteDaySchedule.builder()
                        .routeRecommendation(routeRecommendation)
                        .dayNumber(1)
                        .name("1일차")
                        .build()
        );
        Long savedDayScheduleId = daySchedule.getId();

        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 12, 0);

        RouteScheduleItem nodeItem = RouteScheduleItem.builder()
                .sequence(1)
                .itemType(RouteScheduleItem.ItemType.NODE)
                .nodeType("관광")
                .name("북촌한옥마을")
                .region("서울특별시 종로구")
                .shortComment("전통 한옥 마을을 산책해보세요.")
                .budget("10000")
                .startTime(startTime)
                .endTime(endTime)
                .daySchedule(daySchedule)
                .build();

        // when
        RouteScheduleItem savedItem = routeScheduleItemRepository.save(nodeItem);

        // then
        RouteScheduleItem foundItem = routeScheduleItemRepository.findById(savedItem.getId()).orElse(null);
        assertThat(foundItem).isNotNull();
        assertThat(foundItem.getId()).isEqualTo(savedItem.getId());
        assertThat(foundItem.getSequence()).isEqualTo(1);
        assertThat(foundItem.getItemType()).isEqualTo(RouteScheduleItem.ItemType.NODE);
        assertThat(foundItem.getNodeType()).isEqualTo("관광");
        assertThat(foundItem.getName()).isEqualTo("북촌한옥마을");
        assertThat(foundItem.getRegion()).isEqualTo("서울특별시 종로구");
        assertThat(foundItem.getShortComment()).isEqualTo("전통 한옥 마을을 산책해보세요.");
        assertThat(foundItem.getBudget()).isEqualTo("10000");
        assertThat(foundItem.getStartTime()).isEqualTo(startTime);
        assertThat(foundItem.getEndTime()).isEqualTo(endTime);
    }

    @Test
    @DisplayName("RouteScheduleItem - EDGE 타입 저장 및 조회")
    void saveAndFindEdgeItem() {
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        // given
        RouteDaySchedule daySchedule = routeDayScheduleRepository.save(
                RouteDaySchedule.builder()
                        .routeRecommendation(routeRecommendation)
                        .dayNumber(1)
                        .name("1일차")
                        .build()
        );

        RouteScheduleItem edgeItem = RouteScheduleItem.builder()
                .sequence(2)
                .itemType(RouteScheduleItem.ItemType.EDGE)
                .transportType("도보")
                .travelMinutes(15)
                .description("북촌한옥마을에서 경복궁까지 도보 15분")
                .daySchedule(daySchedule)
                .build();

        // when
        RouteScheduleItem savedItem = routeScheduleItemRepository.save(edgeItem);

        // then
        RouteScheduleItem foundItem = routeScheduleItemRepository.findById(savedItem.getId()).orElse(null);
        assertThat(foundItem).isNotNull();
        assertThat(foundItem.getId()).isEqualTo(savedItem.getId());
        assertThat(foundItem.getSequence()).isEqualTo(2);
        assertThat(foundItem.getItemType()).isEqualTo(RouteScheduleItem.ItemType.EDGE);
        assertThat(foundItem.getTransportType()).isEqualTo("도보");
        assertThat(foundItem.getTravelMinutes()).isEqualTo(15);
        assertThat(foundItem.getDescription()).isEqualTo("북촌한옥마을에서 경복궁까지 도보 15분");
    }

    @Test
    @DisplayName("RouteScheduleItem - dayScheduleId로 조회")
    void findByDayScheduleId() {
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        // given
        RouteDaySchedule daySchedule = routeDayScheduleRepository.save(
                RouteDaySchedule.builder()
                        .routeRecommendation(routeRecommendation)
                        .dayNumber(2)
                        .name("2일차")
                        .build()
        );
        Long savedDayScheduleId = daySchedule.getId();

        LocalDateTime startTime = LocalDateTime.of(2024, 2, 1, 14, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 2, 1, 15, 30);

        RouteScheduleItem item1 = RouteScheduleItem.builder()
                .sequence(1)
                .itemType(RouteScheduleItem.ItemType.NODE)
                .nodeType("식사")
                .name("경동시장")
                .region("서울특별시 종로구")
                .startTime(startTime)
                .endTime(endTime)
                .daySchedule(daySchedule)
                .build();

        RouteScheduleItem item2 = RouteScheduleItem.builder()
                .sequence(2)
                .itemType(RouteScheduleItem.ItemType.EDGE)
                .transportType("지하철")
                .travelMinutes(10)
                .description("경동시장에서 인사동까지")
                .daySchedule(daySchedule)
                .build();

        routeScheduleItemRepository.save(item1);
        routeScheduleItemRepository.save(item2);

        // when
        java.util.List<RouteScheduleItem> items = routeScheduleItemRepository.findByDayScheduleId(savedDayScheduleId);

        // then
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getSequence()).isEqualTo(1);
        assertThat(items.get(1).getSequence()).isEqualTo(2);
    }

    @Test
    @DisplayName("RouteScheduleItem - 삭제 테스트")
    void deleteTest() {
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .requestId(1L)
                .userId(1L)
                .name("서울 여행 추천")
                .recommendationScore(4.8)
                .shortComment("서울의 명소를 효율적으로 돌아보는 코스")
                .budget("50000")
                .region("서울특별시")
                .build();
        routeRecommendationRepository.save(routeRecommendation);

        // given
        RouteDaySchedule daySchedule = routeDayScheduleRepository.save(
                RouteDaySchedule.builder()
                        .routeRecommendation(routeRecommendation)
                        .dayNumber(3)
                        .name("3일차")
                        .build()
        );

        RouteScheduleItem item = RouteScheduleItem.builder()
                .sequence(1)
                .itemType(RouteScheduleItem.ItemType.NODE)
                .nodeType("숙소")
                .name("호텔")
                .daySchedule(daySchedule)
                .build();

        RouteScheduleItem savedItem = routeScheduleItemRepository.save(item);
        Long itemId = savedItem.getId();

        // when
        routeScheduleItemRepository.deleteById(itemId);

        // then
        assertThat(routeScheduleItemRepository.findById(itemId)).isEmpty();
    }
}


package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.plan.domain.RouteScheduleItem.ItemType;

import static org.assertj.core.api.Assertions.assertThat;

class RouteDayScheduleTest {

    @Test
    @DisplayName("addScheduleItem 메서드를 호출하면 항목이 추가되고 양방향 관계가 설정된다 (NODE 타입)")
    void addScheduleItemAsNode() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .name("테스트 여행")
                .build();

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .routeRecommendation(routeRecommendation)
                .build();

        RouteScheduleItem item = RouteScheduleItem.builder()
                .itemType(ItemType.NODE)
                .name("테스트 장소")
                .sequence(1)
                .build();

        // when
        daySchedule.addScheduleItem(item);

        // then
        assertThat(daySchedule.getScheduleItems()).hasSize(1);
        assertThat(daySchedule.getScheduleItems().get(0)).isSameAs(item);
        assertThat(item.getDaySchedule()).isSameAs(daySchedule);
        assertThat(item.getItemType()).isEqualTo(ItemType.NODE);
    }

    @Test
    @DisplayName("addScheduleItem 메서드를 호출하면 항목이 추가되고 양방향 관계가 설정된다 (EDGE 타입)")
    void addScheduleItemAsEdge() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .name("테스트 여행")
                .build();

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .routeRecommendation(routeRecommendation)
                .build();

        RouteScheduleItem item = RouteScheduleItem.builder()
                .itemType(ItemType.EDGE)
                .sequence(1)
                .transportType("도보")
                .travelMinutes(10)
                .build();

        // when
        daySchedule.addScheduleItem(item);

        // then
        assertThat(daySchedule.getScheduleItems()).hasSize(1);
        assertThat(daySchedule.getScheduleItems().get(0)).isSameAs(item);
        assertThat(item.getDaySchedule()).isSameAs(daySchedule);
        assertThat(item.getItemType()).isEqualTo(ItemType.EDGE);
    }

    @Test
    @DisplayName("setRouteRecommendation 메서드를 호출하면 routeRecommendation이 설정된다")
    void setRouteRecommendation() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .name("테스트 여행")
                .build();

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .build();

        // when
        daySchedule.setRouteRecommendation(routeRecommendation);

        // then
        assertThat(daySchedule.getRouteRecommendation()).isSameAs(routeRecommendation);
    }

    @Test
    @DisplayName("빌더를 통해 생성 시 scheduleItems는 빈 리스트로 초기화된다")
    void builderInitialization() {
        // given & when
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .name("테스트 여행")
                .build();

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .routeRecommendation(routeRecommendation)
                .build();

        // then
        assertThat(daySchedule.getScheduleItems()).hasSize(0);
    }

    @Test
    @DisplayName("addScheduleItem를 여러 번 호출하면 항목이 순서대로 추가된다")
    void addMultipleScheduleItems() {
        // given
        RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                .name("테스트 여행")
                .build();

        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .routeRecommendation(routeRecommendation)
                .build();

        RouteScheduleItem item1 = RouteScheduleItem.builder()
                .itemType(ItemType.NODE)
                .name("장소1")
                .sequence(1)
                .build();
        RouteScheduleItem item2 = RouteScheduleItem.builder()
                .itemType(ItemType.EDGE)
                .sequence(2)
                .transportType("지하철")
                .build();
        RouteScheduleItem item3 = RouteScheduleItem.builder()
                .itemType(ItemType.NODE)
                .name("장소3")
                .sequence(3)
                .build();

        // when
        daySchedule.addScheduleItem(item1);
        daySchedule.addScheduleItem(item2);
        daySchedule.addScheduleItem(item3);

        // then
        assertThat(daySchedule.getScheduleItems()).hasSize(3);
        assertThat(daySchedule.getScheduleItems().get(0).getItemType()).isEqualTo(ItemType.NODE);
        assertThat(daySchedule.getScheduleItems().get(0).getName()).isEqualTo("장소1");
        assertThat(daySchedule.getScheduleItems().get(1).getItemType()).isEqualTo(ItemType.EDGE);
        assertThat(daySchedule.getScheduleItems().get(1).getTransportType()).isEqualTo("지하철");
        assertThat(daySchedule.getScheduleItems().get(2).getItemType()).isEqualTo(ItemType.NODE);
        assertThat(daySchedule.getScheduleItems().get(2).getName()).isEqualTo("장소3");
    }
}

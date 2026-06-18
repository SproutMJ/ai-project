// RouteScheduleItemTest.java
package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RouteScheduleItemTest {

    @Test
    @DisplayName("RouteScheduleItem - NODE 타입 빌더 생성 검증")
    void buildNodeItem() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 12, 0);

        // when
        RouteScheduleItem item = RouteScheduleItem.builder()
                .sequence(1)
                .itemType(RouteScheduleItem.ItemType.NODE)
                .nodeType("관광")
                .name("북촌한옥마을")
                .region("서울특별시 종로구")
                .shortComment("전통 한옥 마을을 산책해보세요.")
                .budget("10000")
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // then
        assertThat(item.getSequence()).isEqualTo(1);
        assertThat(item.getItemType()).isEqualTo(RouteScheduleItem.ItemType.NODE);
        assertThat(item.getNodeType()).isEqualTo("관광");
        assertThat(item.getName()).isEqualTo("북촌한옥마을");
        assertThat(item.getRegion()).isEqualTo("서울특별시 종로구");
        assertThat(item.getShortComment()).isEqualTo("전통 한옥 마을을 산책해보세요.");
        assertThat(item.getBudget()).isEqualTo("10000");
        assertThat(item.getStartTime()).isEqualTo(startTime);
        assertThat(item.getEndTime()).isEqualTo(endTime);
        assertThat(item.getTransportType()).isNull();
        assertThat(item.getTravelMinutes()).isNull();
        assertThat(item.getDescription()).isNull();
    }

    @Test
    @DisplayName("RouteScheduleItem - EDGE 타입 빌더 생성 검증")
    void buildEdgeItem() {
        // given
        // when
        RouteScheduleItem item = RouteScheduleItem.builder()
                .sequence(2)
                .itemType(RouteScheduleItem.ItemType.EDGE)
                .transportType("도보")
                .travelMinutes(15)
                .description("북촌한옥마을에서 경복궁까지 도보 15분")
                .build();

        // then
        assertThat(item.getSequence()).isEqualTo(2);
        assertThat(item.getItemType()).isEqualTo(RouteScheduleItem.ItemType.EDGE);
        assertThat(item.getTransportType()).isEqualTo("도보");
        assertThat(item.getTravelMinutes()).isEqualTo(15);
        assertThat(item.getDescription()).isEqualTo("북촌한옥마을에서 경복궁까지 도보 15분");
        assertThat(item.getNodeType()).isNull();
        assertThat(item.getName()).isNull();
        assertThat(item.getStartTime()).isNull();
        assertThat(item.getEndTime()).isNull();
    }

    @Test
    @DisplayName("RouteScheduleItem - setDaySchedule 연관관계 설정 검증")
    void setDaySchedule() {
        // given
        RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                .dayNumber(1)
                .name("1일차")
                .build();

        RouteScheduleItem item = RouteScheduleItem.builder()
                .sequence(1)
                .itemType(RouteScheduleItem.ItemType.NODE)
                .name("경복궁")
                .build();

        // when
        item.setDaySchedule(daySchedule);

        // then
        assertThat(item).isNotNull();
    }

    @Test
    @DisplayName("RouteScheduleItem - ItemType enum 검증")
    void itemTypeEnum() {
        // when & then
        assertThat(RouteScheduleItem.ItemType.NODE).isNotNull();
        assertThat(RouteScheduleItem.ItemType.EDGE).isNotNull();
        assertThat(RouteScheduleItem.ItemType.values().length).isEqualTo(2);
    }

    @Test
    @DisplayName("RouteScheduleItem - 전체 필드 포함 빌더 생성 검증")
    void buildCompleteItem() {
        // given
        LocalDateTime startTime = LocalDateTime.of(2024, 3, 15, 9, 30);
        LocalDateTime endTime = LocalDateTime.of(2024, 3, 15, 11, 0);

        // when
        RouteScheduleItem nodeItem = RouteScheduleItem.builder()
                .id(1L)
                .sequence(1)
                .itemType(RouteScheduleItem.ItemType.NODE)
                .nodeType("관광")
                .name("경복궁")
                .region("서울특별시 종로구")
                .shortComment("대한민국의 대표적인 궁궐")
                .budget("3000")
                .startTime(startTime)
                .endTime(endTime)
                .build();

        RouteScheduleItem edgeItem = RouteScheduleItem.builder()
                .id(2L)
                .sequence(2)
                .itemType(RouteScheduleItem.ItemType.EDGE)
                .transportType("버스")
                .travelMinutes(20)
                .description("경복궁에서 인사동까지 버스 이동")
                .build();

        // then
        assertThat(nodeItem.getId()).isEqualTo(1L);
        assertThat(nodeItem.getItemType()).isEqualTo(RouteScheduleItem.ItemType.NODE);
        assertThat(edgeItem.getId()).isEqualTo(2L);
        assertThat(edgeItem.getItemType()).isEqualTo(RouteScheduleItem.ItemType.EDGE);
    }
}

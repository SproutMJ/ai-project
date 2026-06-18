
package org.mj.trip.plan.domain;

import jakarta.persistence.*;
import lombok.*;
import org.mj.trip.common.entity.BaseTimeEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_schedule_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RouteScheduleItem extends BaseTimeEntity {

    public enum ItemType {
        NODE, EDGE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer sequence; // 일정 순서

    @Column(nullable = false, length = 10)
    private ItemType itemType; // NODE 또는 EDGE

    // --- NODE 전용 필드 ---
    @Column(length = 50)
    private String nodeType; // 관광, 식사, 쇼핑, 숙소 등

    @Column(length = 100)
    private String name;

    @Column(length = 255)
    private String region;

    @Column(length = 500)
    private String shortComment;

    @Column(length = 100)
    private String budget;

    @Column()
    private LocalDateTime startTime; // 체류/소요 시간

    @Column()
    private LocalDateTime endTime; // 체류/소요 시간

    // --- EDGE 전용 필드 ---
    @Column(length = 50)
    private String transportType; // 도보, 버스, 지하철, 택시 등

    @Column
    private Integer travelMinutes; // 이동 시간

    @Column(length = 255)
    private String description;

    // --- 양방향 관계 ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_schedule_id", nullable = false)
    private RouteDaySchedule daySchedule;

    public void setDaySchedule(RouteDaySchedule daySchedule) {
        this.daySchedule = daySchedule;
    }
}

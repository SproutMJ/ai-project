package org.mj.trip.plan.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.mj.trip.common.entity.BaseTimeEntity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "route_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RouteRecommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Double recommendationScore;

    @Column(nullable = false, length = 300)
    private String shortComment;

    @Column(length = 100)
    private String budget;

    @Column(nullable = false, length = 255)
    private String region;

    @OneToMany(mappedBy = "routeRecommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dayNumber ASC")
    @Builder.Default
    @Fetch(FetchMode.SUBSELECT)
    private List<RouteDaySchedule> daySchedules = new ArrayList<>();

    public void addDaySchedule(RouteDaySchedule daySchedule) {
        daySchedules.add(daySchedule);
        daySchedule.setRouteRecommendation(this);
        daySchedules.sort(Comparator.comparing(RouteDaySchedule::getDayNumber));
    }
}

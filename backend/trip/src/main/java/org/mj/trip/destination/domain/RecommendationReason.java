package org.mj.trip.destination.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "RECOMMENDATION_REASON")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationReason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReasonType type;

    @Column(nullable = false)
    private String text;

    @Builder
    public RecommendationReason(Long recommendationId, ReasonType type, String text) {
        this.recommendationId = recommendationId;
        this.type = type;
        this.text = text;
    }
}

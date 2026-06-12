package org.mj.trip.destination.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "RECOMMENDATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long recommendationRequestId;

    @Column(nullable = false)
    private Long destinationId;

    @Column(nullable = false)
    private String destinationName;

    @Column(nullable = false)
    private Double score;

    @Column(nullable = false)
    private Integer rankOrder;

    @Column(nullable = false, length = 1000)
    private String reasonSummary;

    @Builder
    public Recommendation(Long id, Long recommendationRequestId, Long destinationId,
                          String destinationName, Double score, Integer rankOrder,
                          String reasonSummary) {
        this.id = id;
        this.recommendationRequestId = recommendationRequestId;
        this.destinationId = destinationId;
        this.destinationName = destinationName;
        this.score = score;
        this.rankOrder = rankOrder;
        this.reasonSummary = reasonSummary;
    }
}

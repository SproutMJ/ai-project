package org.mj.trip.destination.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "RECOMMENDATION_REQUEST")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long recommendationRequestId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String tripPurpose;

    @Column(nullable = false)
    private String budgetRange;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String season;

    @Column(nullable = false)
    private Integer companionCount;

    @Column(nullable = false)
    private Integer durationDays;

    private LocalDateTime createdAt;

    @Builder
    public RecommendationRequest(Long memberId, String tripPurpose, String budgetRange,
                                 String region, String season, Integer companionCount,
                                 Integer durationDays) {
        this.memberId = memberId;
        this.tripPurpose = tripPurpose;
        this.budgetRange = budgetRange;
        this.region = region;
        this.season = season;
        this.companionCount = companionCount;
        this.durationDays = durationDays;
        this.createdAt = LocalDateTime.now();
    }
}

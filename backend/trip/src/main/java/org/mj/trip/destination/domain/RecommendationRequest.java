package org.mj.trip.destination.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "RECOMMENDATION_REQUEST")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
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

    @Column(nullable = false, length = 1000)
    private String summary;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.summary == null) {
            this.summary = "";
        }
    }

}

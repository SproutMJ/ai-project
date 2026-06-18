package org.mj.trip.pointrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.mj.trip.common.entity.BaseTimeEntity;

@Entity
@Table(name = "recommendation_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointRecommendationRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Lob
    @Column(nullable = false)
    private String requestText;
}

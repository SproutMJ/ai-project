package org.mj.trip.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MEMBER_TRAVEL_STYLE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberTravelStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "travel_style_id", nullable = false)
    private Long travelStyleId;

    public MemberTravelStyle(Long memberId, Long travelStyleId) {
        this.memberId = memberId;
        this.travelStyleId = travelStyleId;
    }
}

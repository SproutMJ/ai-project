package org.mj.trip.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TRAVEL_STYLE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TravelStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    public TravelStyle(String name) {
        this.name = name;
    }
}

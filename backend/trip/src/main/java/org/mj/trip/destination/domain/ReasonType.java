package org.mj.trip.destination.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReasonType {
    BUDGET_MATCH("예산 매칭"),
    SEASON_MATCH("계절 매칭"),
    TRAVEL_STYLE_MATCH("여행 스타일 매칭"),
    DURATION_MATCH("일정 매칭"),
    COMPANION_MATCH("동반자 매칭"),
    POPULARITY("인기 기반"),
    UNIQUE_EXPERIENCE("독특한 경험");

    private final String description;
}

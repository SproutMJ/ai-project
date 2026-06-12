package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TripPlan Entity Tests")
class TripPlanTest {

    @Test
    @DisplayName("should create TripPlan and set/get all fields correctly")
    void testCreateTripPlanAndSetGetFields() {
        TripPlan tripPlan = TripPlan.create(
                1L,
                LocalDate.of(2026, 05, 04),
                LocalDate.of(2026, 05, 06),
                BigDecimal.valueOf(1_000_000L),
                "도쿄",
                2,
                "맛집탐방",
                "대중교통",
                "현지식",
                "여유롭게",
                List.of("ATTRACTION", "RESTAURANT")
        );

        // Basic fields that should be null initially
        assertThat(tripPlan.getId()).isNull();
        assertThat(tripPlan.getTripPlanRequestId()).isNull();
        assertThat(tripPlan.getSummaryText()).isNull();
        assertThat(tripPlan.getSummaryKeyPoints()).isNull();
        assertThat(tripPlan.getTravelStyleIds()).isNull();
        assertThat(tripPlan.getTitle()).isNull();
        assertThat(tripPlan.getPlanData()).isNull();
        assertThat(tripPlan.getUpdatedAt()).isNull();
        assertThat(tripPlan.getDeletedAt()).isNull();

        // Status should be set to DRAFT by default
        assertThat(tripPlan.getStatus()).isEqualTo("DRAFT");

        // Core fields set by create method
        assertThat(tripPlan.getMemberId()).isEqualTo(1L);
        assertThat(tripPlan.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 4));
        assertThat(tripPlan.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 6));
        assertThat(tripPlan.getBudgetAmount()).isEqualByComparingTo(BigDecimal.valueOf(1_000_000L));
        assertThat(tripPlan.getRegion()).isEqualTo("도쿄");
        assertThat(tripPlan.getCompanionCount()).isEqualTo(2);
        assertThat(tripPlan.getTripPurpose()).isEqualTo("맛집탐방");
        assertThat(tripPlan.getTransportMode()).isEqualTo("대중교통");
        assertThat(tripPlan.getMealPreference()).isEqualTo("현지식");
        assertThat(tripPlan.getPaceLevel()).isEqualTo("여유롭게");
        assertThat(tripPlan.getPriorityTypes()).isEqualTo("ATTRACTION,RESTAURANT");
        
        // createdAt should be set and not null
        assertThat(tripPlan.getCreatedAt()).isNotNull();
        assertThat(tripPlan.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("should set and get memberId correctly")
    void testSetAndGetMemberId() {
        TripPlan tripPlan = TripPlan.create(
                42L,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 5),
                BigDecimal.valueOf(500_000L),
                "오사카",
                1,
                "관광",
                "자가용",
                "한식",
                "빠르게",
                List.of("MUSEUM")
        );

        assertThat(tripPlan.getMemberId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("should handle empty priorityTypes list")
    void testCreateWithEmptyPriorityTypes() {
        TripPlan tripPlan = TripPlan.create(
                1L,
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 3),
                BigDecimal.valueOf(2_000_000L),
                "제주도",
                3,
                "휴식",
                "렌트카",
                "무관",
                "여유롭게",
                List.of()
        );

        assertThat(tripPlan.getPriorityTypes()).isEmpty();
        assertThat(tripPlan.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("should handle single priorityType")
    void testCreateWithSinglePriorityType() {
        TripPlan tripPlan = TripPlan.create(
                5L,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                BigDecimal.valueOf(100_000L),
                "서울",
                1,
                "쇼핑",
                "대중교통",
                "무관",
                "빠르게",
                List.of("SHOPPING")
        );

        assertThat(tripPlan.getPriorityTypes()).isEqualTo("SHOPPING");
    }
}

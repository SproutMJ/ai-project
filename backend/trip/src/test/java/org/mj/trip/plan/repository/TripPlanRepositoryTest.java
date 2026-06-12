package org.mj.trip.plan.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.plan.domain.TripPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TripPlanRepository 테스트")
class TripPlanRepositoryTest {

    @Autowired
    private TripPlanRepository tripPlanRepository;

    @Test
    @DisplayName("성공: TripPlan 엔티티 저장")
    void save_success() {
        // given
        TripPlan tripPlan = TripPlan.builder()
                .tripPlanRequestId(1001L)
                .status("DRAFT")
                .summaryText("테스트 일정")
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 5))
                .budgetAmount(BigDecimal.valueOf(1000000))
                .region("도쿄")
                .companionCount(2)
                .tripPurpose("맛집탐방")
                .transportMode("대중교통")
                .mealPreference("현지식")
                .paceLevel("여유롭게")
                .priorityTypes("ATTRACTION,RESTAURANT")
                .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0, 0))
                .build();

        // when
        TripPlan saved = tripPlanRepository.save(tripPlan);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("성공: 전체 목록 조회 - 페이지네이션 적용")
    void findAll_withPagination_success() {
        // given
        for (int i = 1; i <= 25; i++) {
            TripPlan tripPlan = TripPlan.builder()
                    .tripPlanRequestId((long) (1000 + i))
                    .status("DRAFT")
                    .summaryText("테스트 일정 " + i)
                    .startDate(LocalDate.of(2026, 5, 1))
                    .endDate(LocalDate.of(2026, 5, 5))
                    .budgetAmount(BigDecimal.valueOf(1000000))
                    .region("도쿄")
                    .companionCount(2)
                    .tripPurpose("맛집탐방")
                    .transportMode("대중교통")
                    .mealPreference("현지식")
                    .paceLevel("여유롭게")
                    .priorityTypes("ATTRACTION,RESTAURANT")
                    .createdAt(LocalDateTime.of(2026, 4, 21, 10, 0, 0))
                    .build();
            tripPlanRepository.save(tripPlan);
        }

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<TripPlan> page = tripPlanRepository.findAll(pageable);

        // then
        assertThat(page.getContent()).hasSize(20);
        assertThat(page.getTotalElements()).isEqualTo(25);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("성공: 상태별 조회 - 단일 상태")
    void findByStatus_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("ACTIVE", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("DRAFT", "오사카", LocalDateTime.of(2026, 4, 23, 10, 0, 0)));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.findByStatus("DRAFT", pageable);

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(tp -> "DRAFT".equals(tp.getStatus()));
    }

    @Test
    @DisplayName("성공: 상태별 조회 - 다중 상태")
    void findByStatusIn_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("ACTIVE", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("ARCHIVED", "도쿄", LocalDateTime.of(2026, 4, 23, 10, 0, 0)));

        List<String> statuses = List.of("DRAFT", "ACTIVE");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.findByStatusIn(statuses, pageable);

        // then
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(tp -> statuses.contains(tp.getStatus()));
    }

    @Test
    @DisplayName("성공: 조건부 필터 조회 - 상태 필터")
    void applyFilters_withStatus_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("ACTIVE", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0)));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.applyFilters(
                "DRAFT", null, null, null, null, null, pageable
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStatus()).isEqualTo("DRAFT");
    }

    @Test
    @DisplayName("성공: 조건부 필터 조회 - 지역 필터")
    void applyFilters_withRegion_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("DRAFT", "오사카", LocalDateTime.of(2026, 4, 22, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("DRAFT", "교토", LocalDateTime.of(2026, 4, 23, 10, 0, 0)));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.applyFilters(
                null, "도쿄", null, null, null, null, pageable
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getRegion()).isEqualTo("도쿄");
    }

    @Test
    @DisplayName("성공: 조건부 필터 조회 - 시작 날짜 범위 필터")
    void applyFilters_withStartDateRange_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0),
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 3)));
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0),
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 3)));
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 23, 10, 0, 0),
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3)));

        LocalDate startDateFrom = LocalDate.of(2026, 5, 1);
        LocalDate startDateTo = LocalDate.of(2026, 5, 31);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.applyFilters(
                null, null, startDateFrom, startDateTo, null, null, pageable
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    @DisplayName("성공: 조건부 필터 조회 - 생성 일시 범위 필터")
    void applyFilters_withCreatedAtRange_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 23, 10, 0, 0)));

        LocalDateTime createdFrom = LocalDateTime.of(2026, 4, 22, 0, 0, 0);
        LocalDateTime createdTo = LocalDateTime.of(2026, 4, 22, 23, 59, 59);
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.applyFilters(
                null, null, null, null, createdFrom, createdTo, pageable
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 22, 10, 0, 0));
    }

    @Test
    @DisplayName("성공: 조건부 필터 조회 - 모든 필터 조합")
    void applyFilters_withAllFilters_success() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0),
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)));
        tripPlanRepository.save(createTripPlan("ACTIVE", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0),
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)));
        tripPlanRepository.save(createTripPlan("DRAFT", "오사카", LocalDateTime.of(2026, 4, 21, 10, 0, 0),
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5)));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.applyFilters(
                "DRAFT", "도쿄", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5),
                LocalDateTime.of(2026, 4, 20, 0, 0, 0), LocalDateTime.of(2026, 4, 22, 0, 0, 0), pageable
        );

        // then
        assertThat(page.getContent()).hasSize(1);
        TripPlan result = page.getContent().get(0);
        assertThat(result.getStatus()).isEqualTo("DRAFT");
        assertThat(result.getRegion()).isEqualTo("도쿄");
        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 4, 21, 10, 0, 0));
    }

    @Test
    @DisplayName("성공: 조건부 필터 조회 - 필터 조건 없음 시 전체 조회")
    void applyFilters_withNullFilters_returnsAll() {
        // given
        tripPlanRepository.save(createTripPlan("DRAFT", "도쿄", LocalDateTime.of(2026, 4, 21, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("ACTIVE", "도쿄", LocalDateTime.of(2026, 4, 22, 10, 0, 0)));
        tripPlanRepository.save(createTripPlan("DRAFT", "오사카", LocalDateTime.of(2026, 4, 23, 10, 0, 0)));

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<TripPlan> page = tripPlanRepository.applyFilters(
                null, null, null, null, null, null, pageable
        );

        // then
        assertThat(page.getContent()).hasSize(3);
    }

    // ==================== 헬퍼 메소드 ====================

    private TripPlan createTripPlan(String status, String region, LocalDateTime createdAt) {
        return createTripPlan(status, region, createdAt, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 5));
    }

    private TripPlan createTripPlan(String status, String region, LocalDateTime createdAt,
                                    LocalDate startDate, LocalDate endDate) {
        return TripPlan.builder()
                .tripPlanRequestId((long) System.currentTimeMillis())
                .status(status)
                .summaryText("테스트 일정")
                .startDate(startDate)
                .endDate(endDate)
                .budgetAmount(BigDecimal.valueOf(1000000))
                .region(region)
                .companionCount(2)
                .tripPurpose("맛집탐방")
                .transportMode("대중교통")
                .mealPreference("현지식")
                .paceLevel("여유롭게")
                .priorityTypes("ATTRACTION,RESTAURANT")
                .createdAt(createdAt)
                .build();
    }
}

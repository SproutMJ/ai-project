package org.mj.trip.plan.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
@Entity
@Table(name = "trip_plan")
public class TripPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId; // 추가: MEMBER FK
    private Long tripPlanRequestId;
    private String title; // 추가: 여행 제목
    private String status;
    private String summaryText; // API의 summary.text에 매핑
    private String summaryKeyPoints;
    private LocalDate startDate; // API 명세서 기준 String 또는 LocalDate로 변경 가능하지만, 기존 create 메서드 호환을 위해 String 유지 (또는 LocalDate)
    private LocalDate endDate;
    private BigDecimal budgetAmount;
    private String region;
    private Integer companionCount;
    private String tripPurpose;
    private String transportMode;
    private String mealPreference;
    private String paceLevel;
    private String priorityTypes;
    private String travelStyleIds;
    private String planData; // JSON 형태 일정 데이터 저장
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static TripPlan create(
            Long memberId,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal budgetAmount,
            String region,
            Integer companionCount,
            String tripPurpose,
            String transportMode,
            String mealPreference,
            String paceLevel,
            List<String> priorityTypes
    ) {
        TripPlan tripPlan = new TripPlan();
        tripPlan.memberId = memberId;
        tripPlan.startDate = startDate;
        tripPlan.endDate = endDate;
        tripPlan.budgetAmount = budgetAmount;
        tripPlan.region = region;
        tripPlan.companionCount = companionCount;
        tripPlan.tripPurpose = tripPurpose;
        tripPlan.transportMode = transportMode;
        tripPlan.mealPreference = mealPreference;
        tripPlan.paceLevel = paceLevel;
        tripPlan.priorityTypes = String.join(",", priorityTypes);
        tripPlan.status = "DRAFT";
        tripPlan.createdAt = LocalDateTime.now();
        return tripPlan;
    }


}


package org.mj.trip.plan.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.mj.trip.plan.domain.TripPlan;

import com.fasterxml.jackson.annotation.JsonFormat;

public class TripPlanListResponse {

    private List<TripPlanItem> data;
    private Meta meta;

    public TripPlanListResponse(List<TripPlanItem> data, Meta meta) {
        this.data = data;
        this.meta = meta;
    }

    // ... existing code ...

    public static TripPlanListResponse from(List<TripPlan> tripPlans, org.springframework.data.domain.Page<TripPlan> page) {
        List<TripPlanItem> items = tripPlans.stream()
                .map(TripPlanItem::from)
                .collect(Collectors.toList());

        Meta meta = new Meta(
                page.getNumber() + 1, // 0-based index를 1-based로 변환
                page.getSize(),
                (int) page.getTotalElements(),
                page.getTotalPages()
        );

        return new TripPlanListResponse(items, meta);
    }

    public static class TripPlanItem {
        private Long tripPlanId;
        private String region;
        private String status;
        private String summaryText;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", shape = JsonFormat.Shape.STRING)
        private LocalDateTime createdAt;

        public TripPlanItem(Long tripPlanId, String region, String status, String summaryText, LocalDateTime createdAt) {
            this.tripPlanId = tripPlanId;
            this.region = region;
            this.status = status;
            this.summaryText = summaryText;
            this.createdAt = createdAt;
        }

        public static TripPlanItem from(TripPlan tripPlan) {
            return new TripPlanItem(
                    tripPlan.getId(),
                    tripPlan.getRegion(),
                    tripPlan.getStatus(),
                    tripPlan.getSummaryText(),
                    tripPlan.getCreatedAt()
            );
        }

        // Getters
        public Long getTripPlanId() { return tripPlanId; }
        public String getRegion() { return region; }
        public String getStatus() { return status; }
        public String getSummaryText() { return summaryText; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    public static class Meta {
        private int page;
        private int size;
        private int totalElements;
        private int totalPages;

        public Meta(int page, int size, int totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        // Getters
        public int getPage() { return page; }
        public int getSize() { return size; }
        public int getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
    }

    // Getters for root
    public List<TripPlanItem> getData() { return data; }
    public Meta getMeta() { return meta; }
}

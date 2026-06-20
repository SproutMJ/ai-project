package org.mj.trip.plan.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record ScheduleRequestsResponseDto(
        List<ScheduleRequestResponseDto> scheduleRequests,
        Meta meta
) {
    public record Meta(
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static Meta from(Page<?> page) {
            return new Meta(
                    page.getNumber() + 1,
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages()
            );
        }
    }
}


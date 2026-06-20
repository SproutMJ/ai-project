package org.mj.trip.plan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleRequestResponseDto(
        Long id,
        Long userId,
        String requestText,
        LocalDate startDate,
        LocalDate endDate,
        String region,
        BigDecimal budget
) {
}

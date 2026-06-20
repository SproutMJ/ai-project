package org.mj.trip.plan.dto.request;

public record ScheduleRquestsRequestDto(
        int page,
        int size,
        String sort,
        String order
) { }

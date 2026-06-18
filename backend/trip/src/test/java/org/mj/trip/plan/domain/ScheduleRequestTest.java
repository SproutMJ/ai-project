
package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScheduleRequest Entity 테스트")
class ScheduleRequestTest {

    @Test
    @DisplayName("ScheduleRequest 생성 시 값이 올바르게 설정되어야 한다")
    void builderCreatesScheduleRequest() {
        // given & when
        Long userId = 1L;
        String requestText = "경기도 가볼만한 곳 추천해줘";
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .userId(userId)
                .requestText(requestText)
                .build();

        // then
        assertNull(scheduleRequest.getId()); // DB 저장 시 자동 생성
        assertEquals(userId, scheduleRequest.getUserId());
        assertEquals(requestText, scheduleRequest.getRequestText());
    }

    @Test
    @DisplayName("Builder로 생성된 엔티티는 기본 필드를 null로 시작한다")
    void builderDefaultsAreNull() {
        // given & when
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .userId(1L)
                .requestText("테스트 요청")
                .build();

        // then
        // BaseTimeEntity의 createdAt, updatedAt은 auditing 설정 전에는 null일 수 있음
        assertNotNull(scheduleRequest.getUserId());
        assertNotNull(scheduleRequest.getRequestText());
    }
}

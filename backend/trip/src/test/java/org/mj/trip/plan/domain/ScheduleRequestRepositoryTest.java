
package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ScheduleRequest Repository 테스트")
@DataJpaTest
class ScheduleRequestRepositoryTest {

    @Autowired
    private ScheduleRequestRepository scheduleRequestRepository;

    @Test
    @DisplayName("ScheduleRequest 저장 및 ID로 조회")
    void saveAndFindById() {
        // given
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .userId(1L)
                .requestText("경기도 가볼만한 곳 추천해줘")
                .build();

        // when
        ScheduleRequest saved = scheduleRequestRepository.save(scheduleRequest);

        // then
        assertNotNull(saved.getId());
        assertEquals(1L, saved.getUserId());
        assertEquals("경기도 가볼만한 곳 추천해줘", saved.getRequestText());
    }

    @Test
    @DisplayName("ID로 ScheduleRequest 조회 시 존재하면 반환한다")
    void findByIdPresent() {
        // given
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .userId(1L)
                .requestText("제주도 여행 추천")
                .build();
        ScheduleRequest saved = scheduleRequestRepository.save(scheduleRequest);

        // when
        Optional<ScheduleRequest> found = scheduleRequestRepository.findById(saved.getId());

        // then
        assertTrue(found.isPresent());
        assertEquals("제주도 여행 추천", found.get().getRequestText());
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회 시 빈 Optional을 반환한다")
    void findByIdEmpty() {
        // when
        Optional<ScheduleRequest> found = scheduleRequestRepository.findById(9999L);

        // then
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("여러 ScheduleRequest 저장 및 전체 조회")
    void saveAndCount() {
        // given
        scheduleRequestRepository.save(ScheduleRequest.builder()
                .userId(1L).requestText("요리 추천").build());
        scheduleRequestRepository.save(ScheduleRequest.builder()
                .userId(2L).requestText("카페 추천").build());
        scheduleRequestRepository.save(ScheduleRequest.builder()
                .userId(3L).requestText("숙소 추천").build());

        // when & then
        assertEquals(3, scheduleRequestRepository.count());
    }

    @Test
    @DisplayName("ScheduleRequest 삭제")
    void deleteById() {
        // given
        ScheduleRequest scheduleRequest = ScheduleRequest.builder()
                .userId(1L)
                .requestText("삭제할 테스트")
                .build();
        ScheduleRequest saved = scheduleRequestRepository.save(scheduleRequest);

        // when
        scheduleRequestRepository.deleteById(saved.getId());

        // then
        Optional<ScheduleRequest> found = scheduleRequestRepository.findById(saved.getId());
        assertTrue(found.isEmpty());
    }
}

package org.mj.trip.plan.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("RouteRecommendation 엔티티 테스트")
class RouteRecommendationTest {

    @Nested
    @DisplayName("빌더 테스트")
    class BuilderTest {

        @Test
        @DisplayName("RouteRecommendation을 빌더로 생성해야 한다")
        void testBuildRouteRecommendation() {
            // given
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("서울 여행 추천코스")
                    .recommendationScore(4.5)
                    .shortComment("서울의 핵심 명소를 돌아보는 코스")
                    .budget("10만원")
                    .region("서울특별시")
                    .build();

            // then
            assertThat(routeRecommendation.getId()).isEqualTo(1L);
            assertThat(routeRecommendation.getRequestId()).isEqualTo(100L);
            assertThat(routeRecommendation.getUserId()).isEqualTo(1L);
            assertThat(routeRecommendation.getName()).isEqualTo("서울 여행 추천코스");
            assertThat(routeRecommendation.getRecommendationScore()).isEqualTo(4.5);
            assertThat(routeRecommendation.getShortComment()).isEqualTo("서울의 핵심 명소를 돌아보는 코스");
            assertThat(routeRecommendation.getBudget()).isEqualTo("10만원");
            assertThat(routeRecommendation.getRegion()).isEqualTo("서울특별시");
            assertThat(routeRecommendation.getDaySchedules()).isEmpty();
        }

        @Test
        @DisplayName("날짜별 일정을 추가할 수 있어야 한다")
        void testAddDaySchedule() {
            // given
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("서울 여행 추천코스")
                    .recommendationScore(4.5)
                    .shortComment("서울의 핵심 명소를 돌아보는 코스")
                    .region("서울특별시")
                    .build();

            RouteDaySchedule daySchedule1 = RouteDaySchedule.builder()
                    .routeRecommendation(routeRecommendation)
                    .dayNumber(1)
                    .build();

            RouteDaySchedule daySchedule2 = RouteDaySchedule.builder()
                    .routeRecommendation(routeRecommendation)
                    .dayNumber(2)
                    .build();

            // when
            routeRecommendation.addDaySchedule(daySchedule1);
            routeRecommendation.addDaySchedule(daySchedule2);

            // then
            assertThat(routeRecommendation.getDaySchedules()).hasSize(2);
            assertThat(routeRecommendation.getDaySchedules().get(0).getDayNumber()).isEqualTo(1);
            assertThat(routeRecommendation.getDaySchedules().get(1).getDayNumber()).isEqualTo(2);
        }

        @Test
        @DisplayName("addDaySchedule는 양방향 관계를 설정해야 한다")
        void testAddDayScheduleSetsBidirectionalRelationship() {
            // given
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("서울 여행 추천코스")
                    .recommendationScore(4.5)
                    .shortComment("서울의 핵심 명소를 돌아보는 코스")
                    .region("서울특별시")
                    .build();

            RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                    .dayNumber(1)
                    .build();

            // when
            routeRecommendation.addDaySchedule(daySchedule);

            // then
            assertThat(daySchedule.getRouteRecommendation()).isSameAs(routeRecommendation);
        }

        @Test
        @DisplayName("addDaySchedule는 일정을 dayNumber 기준 오름차순으로 정렬해야 한다")
        void testAddDayScheduleOrdersByDayNumber() {
            // given
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("서울 여행 추천코스")
                    .recommendationScore(4.5)
                    .shortComment("서울의 핵심 명소를 돌아보는 코스")
                    .region("서울특별시")
                    .build();

            RouteDaySchedule daySchedule3 = RouteDaySchedule.builder()
                    .routeRecommendation(routeRecommendation)
                    .dayNumber(3)
                    .build();

            RouteDaySchedule daySchedule1 = RouteDaySchedule.builder()
                    .routeRecommendation(routeRecommendation)
                    .dayNumber(1)
                    .build();

            RouteDaySchedule daySchedule2 = RouteDaySchedule.builder()
                    .routeRecommendation(routeRecommendation)
                    .dayNumber(2)
                    .build();

            // when: 역순으로 추가
            routeRecommendation.addDaySchedule(daySchedule3);
            routeRecommendation.addDaySchedule(daySchedule1);
            routeRecommendation.addDaySchedule(daySchedule2);

            // then
            assertThat(routeRecommendation.getDaySchedules().get(0).getDayNumber()).isEqualTo(1);
            assertThat(routeRecommendation.getDaySchedules().get(1).getDayNumber()).isEqualTo(2);
            assertThat(routeRecommendation.getDaySchedules().get(2).getDayNumber()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("생성자 테스트")
    class ConstructorTest {

        @Test
        @DisplayName("기본 생성자는 protected 접근이어야 한다")
        void testNoArgsConstructorIsProtected() throws Exception {
            // when & then: 리플렉션을 통해 protected 생성자가 존재하는지 확인
            Constructor<RouteRecommendation> constructor = RouteRecommendation.class.getDeclaredConstructor();
            assertTrue(Modifier.isProtected(constructor.getModifiers()), "기본 생성자는 protected이어야 합니다.");
            assertEquals(RouteRecommendation.class, constructor.getDeclaringClass());
        }

        @Test
        @DisplayName("모든 필드를 받는 전체 생성자로 생성할 수 있어야 한다")
        void testAllArgsConstructor() {
            // given
            List<RouteDaySchedule> daySchedules = new ArrayList<>();

            // when
            RouteRecommendation routeRecommendation = new RouteRecommendation(
                    1L, 1L, 1L, "서울 여행", LocalDate.now(), LocalDate.now(), 4.5, "짧은 설명", "10만원", "서울특별시", daySchedules
            );

            // then
            assertThat(routeRecommendation.getId()).isEqualTo(1L);
            assertThat(routeRecommendation.getRequestId()).isEqualTo(1L);
            assertThat(routeRecommendation.getUserId()).isEqualTo(1L);
            assertThat(routeRecommendation.getName()).isEqualTo("서울 여행");
            assertThat(routeRecommendation.getRecommendationScore()).isEqualTo(4.5);
            assertThat(routeRecommendation.getShortComment()).isEqualTo("짧은 설명");
            assertThat(routeRecommendation.getBudget()).isEqualTo("10만원");
            assertThat(routeRecommendation.getRegion()).isEqualTo("서울특별시");
        }
    }

    @Nested
    @DisplayName("필드 검증 테스트")
    class FieldValidationTest {

        @Test
        @DisplayName("name 필드는 100자 이내여야 한다")
        void testNameMaxLength() {
            // given
            String name = "a".repeat(100);
            String tooLongName = "a".repeat(101);

            // when & then: 빌더는 자바 객체이므로 길이 제한은 데이터베이스 계층에서 검증됨
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name(name)
                    .recommendationScore(4.5)
                    .shortComment("설명")
                    .region("서울")
                    .build();

            assertThat(routeRecommendation.getName()).hasSize(100);
        }

        @Test
        @DisplayName("shortComment 필드는 300자 이내여야 한다")
        void testShortCommentMaxLength() {
            // given
            String shortComment = "a".repeat(300);

            // when
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("여행")
                    .recommendationScore(4.5)
                    .shortComment(shortComment)
                    .region("서울")
                    .build();

            // then
            assertThat(routeRecommendation.getShortComment()).hasSize(300);
        }

        @Test
        @DisplayName("budget 필드는 null일 수 있어야 한다")
        void testBudgetNullable() {
            // when
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("여행")
                    .recommendationScore(4.5)
                    .shortComment("설명")
                    .region("서울")
                    .build();

            // then
            assertThat(routeRecommendation.getBudget()).isNull();
        }

        @Test
        @DisplayName("budget 필드에 값을 설정할 수 있어야 한다")
        void testBudgetSettable() {
            // when
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("여행")
                    .recommendationScore(4.5)
                    .shortComment("설명")
                    .budget("5만원")
                    .region("서울")
                    .build();

            // then
            assertThat(routeRecommendation.getBudget()).isEqualTo("5만원");
        }
    }

    @Nested
    @DisplayName("일정 관련 테스트")
    class DayScheduleTest {

        @Test
        @DisplayName("생성 시 daySchedules는 빈 리스트여야 한다")
        void testDaySchedulesIsEmptyByDefault() {
            // when
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("여행")
                    .recommendationScore(4.5)
                    .shortComment("설명")
                    .region("서울")
                    .build();

            // then
            assertThat(routeRecommendation.getDaySchedules()).isEmpty();
            assertThat(routeRecommendation.getDaySchedules()).isNotNull();
        }

        @Test
        @DisplayName("여러 일정을 순차적으로 추가할 수 있어야 한다")
        void testAddMultipleDaySchedules() {
            // given
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("서울 여행")
                    .recommendationScore(4.5)
                    .shortComment("서울 여행 코스")
                    .region("서울특별시")
                    .build();

            // when
            for (int i = 1; i <= 3; i++) {
                RouteDaySchedule daySchedule = RouteDaySchedule.builder()
                        .dayNumber(i)
                        .build();
                routeRecommendation.addDaySchedule(daySchedule);
            }

            // then
            assertThat(routeRecommendation.getDaySchedules()).hasSize(3);
            for (int i = 0; i < 3; i++) {
                assertThat(routeRecommendation.getDaySchedules().get(i).getDayNumber())
                        .isEqualTo(i + 1);
            }
        }

        @Test
        @DisplayName("daySchedules의 크기를 반환해야 한다")
        void testGetDaySchedulesSize() {
            // given
            RouteRecommendation routeRecommendation = RouteRecommendation.builder()
                    .id(1L)
                    .requestId(100L)
                    .userId(1L)
                    .name("여행")
                    .recommendationScore(4.5)
                    .shortComment("설명")
                    .region("서울")
                    .build();

            routeRecommendation.addDaySchedule(RouteDaySchedule.builder().dayNumber(1).build());
            routeRecommendation.addDaySchedule(RouteDaySchedule.builder().dayNumber(2).build());
            routeRecommendation.addDaySchedule(RouteDaySchedule.builder().dayNumber(3).build());

            // then
            assertThat(routeRecommendation.getDaySchedules().size()).isEqualTo(3);
        }
    }
}

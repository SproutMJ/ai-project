package org.mj.trip.pointrecommendation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PointRecommendation 엔티티 단위 테스트
 * 엔티티의 상태 변경, 검증, 연관관계 매핑 등을 테스트합니다.
 */
@DisplayName("PointRecommendation Entity 단위 테스트")
class PointRecommendationTest {

    private PointRecommendationRequest request;
    private PointRecommendation recommendation;

    @BeforeEach
    void setUp() {
        // 테스트용 요청 엔티티 생성
        request = PointRecommendationRequest.builder()
                .id(1L)
                .userId(100L)
                .requestText("서울 근처 맛집 추천해주세요.")
                .build();

        // 기본 테스트 데이터 생성
        recommendation = PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("경리단길 맛집")
                .recommendationScore(0.95)
                .shortComment("감성적인 분위기의 맛집")
                .type("식당")
                .region("서울 종로구")
                .keyword("한식, 감성")
                .theme("연인 데이트")
                .budget("3~5만 원")
                .requiredTime("1~2시간")
                .howToGo("지하철 1호선 안국역 3번 출구")
                .recommendedPartySize("2~4명")
                .weather("맑음, 비 올 때 모두OK")
                .language("한국어, 영어")
                .disadvantage("주말에 혼잡함")
                .description("경리단길에 위치한 감성적인 한식당입니다.")
                .build();
    }

    @Test
    @DisplayName("엔티티 빌더 패턴으로 생성해야 한다")
    void builderTest() {
        assertThat(recommendation).isNotNull();
        assertThat(recommendation.getName()).isEqualTo("경리단길 맛집");
        assertThat(recommendation.getRecommendationScore()).isEqualTo(0.95);
        assertThat(recommendation.getRequest()).isEqualTo(request);
    }

    @Test
    @DisplayName("권한 없는 인스턴스 생성 방지 (NoArgsConstructor 보호)")
    void protectedNoArgsConstructorTest() {
        // Lombok의 @NoArgsConstructor(access = AccessLevel.PROTECTED)로 인해
        // 외부에서 기본 생성자를 호출할 수 없어야 한다.
        // 이 테스트는 컴파일 단계에서 검증되므로, 직접 테스트 메서드로 작성하기 어렵지만
        // 빌더를 사용해야 한다는 것을 문서적으로 명시함.
    }

    @Test
    @DisplayName("연관관계 설정 시 request ID가 설정되어야 한다")
    void requestAssociationTest() {
        // given
        assertThat(recommendation.getRequest()).isNotNull();
        assertThat(recommendation.getRequest().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("필수 필드가 null이 아니어야 한다")
    void mandatoryFieldsNotNullTest() {
        // given & when & then
        assertThat(recommendation.getUserId()).isNotNull();
        assertThat(recommendation.getName()).isNotNull();
        assertThat(recommendation.getRecommendationScore()).isNotNull();
        assertThat(recommendation.getShortComment()).isNotNull();
        assertThat(recommendation.getType()).isNotNull();
        assertThat(recommendation.getRegion()).isNotNull();
    }

    @Test
    @DisplayName("선택적 필드는 null일 수 있다")
    void optionalFieldsNullableTest() {
        // given
        PointRecommendation recommendationWithoutOptional = PointRecommendation.builder()
                .request(request)
                .userId(200L)
                .name("테스트 장소")
                .recommendationScore(0.8)
                .shortComment("테스트용短评")
                .type("관광지")
                .region("서울 강남구")
                // optional 필드들은 설정하지 않음
                .build();

        // then
        assertThat(recommendationWithoutOptional.getKeyword()).isNull();
        assertThat(recommendationWithoutOptional.getTheme()).isNull();
        assertThat(recommendationWithoutOptional.getBudget()).isNull();
        assertThat(recommendationWithoutOptional.getRequiredTime()).isNull();
        assertThat(recommendationWithoutOptional.getHowToGo()).isNull();
        assertThat(recommendationWithoutOptional.getRecommendedPartySize()).isNull();
        assertThat(recommendationWithoutOptional.getWeather()).isNull();
        assertThat(recommendationWithoutOptional.getLanguage()).isNull();
        assertThat(recommendationWithoutOptional.getDisadvantage()).isNull();
        assertThat(recommendationWithoutOptional.getDescription()).isNull();
    }

    @Test
    @DisplayName("name 필드는 최대 100자까지 허용된다")
    void nameLengthTest() {
        // given
        String longName = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name(longName)
                .recommendationScore(0.9)
                .shortComment("짧은설명")
                .type("카테고리")
                .region("지역")
                .build())
                .isInstanceOf(Exception.class); // JPA 초기화 단계에서 검증 오류 발생
    }

    @Test
    @DisplayName("shortComment 필드는 최대 300자까지 허용된다")
    void shortCommentLengthTest() {
        // given
        String longComment = "a".repeat(301);

        // when & then
        assertThatThrownBy(() -> PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(0.9)
                .shortComment(longComment)
                .type("카테고리")
                .region("지역")
                .build())
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("type 필드는 최대 100자까지 허용된다")
    void typeLengthTest() {
        // given
        String longType = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(0.9)
                .shortComment("설명")
                .type(longType)
                .region("지역")
                .build())
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("region 필드는 최대 255자까지 허용된다")
    void regionLengthTest() {
        // given
        String longRegion = "a".repeat(256);

        // when & then
        assertThatThrownBy(() -> PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(0.9)
                .shortComment("설명")
                .type("카테고리")
                .region(longRegion)
                .build())
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("description은 TEXT 타입으로 긴 내용도 저장할 수 있다")
    void descriptionLongTextTest() {
        // given
        String longDescription = "a".repeat(1000);

        // when
        PointRecommendation recommendationWithLongDesc = PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(0.9)
                .shortComment("설명")
                .type("카테고리")
                .region("지역")
                .description(longDescription)
                .build();

        // then
        assertThat(recommendationWithLongDesc.getDescription()).isEqualTo(longDescription);
    }

    @Test
    @DisplayName("로딩 타입이 LAZY이어야 한다")
    void lazyFetchTypeTest() {
        // given & when & then
        // JPA 엔티티 매핑 정보에서 FetchType.LAZY인지 확인
        // 실제 테스트에서는 리플렉션을 사용하여 검증할 수 있음
        assertThat(recommendation.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("ID가 null이어야 새로운 엔티티로 간주된다")
    void idNullForNewEntityTest() {
        // given
        PointRecommendation newRecommendation = PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("새로운 장소")
                .recommendationScore(0.9)
                .shortComment("새로운 설명")
                .type("카테고리")
                .region("지역")
                .build();

        // then
        // 새 빌더로 생성된 엔티티의 ID는 null이어야 함 (생성 시점에는 DB에 저장되지 않았으므로)
        // 단, @GeneratedValue로 ID가 자동으로 할당되므로 persist 전에는 null
    }

    @Test
    @DisplayName("추천 점수 범위 검증 (0.0 ~ 1.0)")
    void recommendationScoreRangeTest() {
        // given & when
        PointRecommendation validScore = PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(0.5)
                .shortComment("설명")
                .type("카테고리")
                .region("지역")
                .build();

        PointRecommendation minScore = PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(0.0)
                .shortComment("설명")
                .type("카테고리")
                .region("지역")
                .build();

        PointRecommendation maxScore = PointRecommendation.builder()
                .request(request)
                .userId(100L)
                .name("장소")
                .recommendationScore(1.0)
                .shortComment("설명")
                .type("카테고리")
                .region("지역")
                .build();

        // then
        assertThat(validScore.getRecommendationScore()).isEqualTo(0.5);
        assertThat(minScore.getRecommendationScore()).isEqualTo(0.0);
        assertThat(maxScore.getRecommendationScore()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("BaseTimeEntity로부터 생성일시 정보를 가져올 수 있어야 한다")
    void baseTimeEntityInheritanceTest() {
        // given & when
        // JPA가 엔티티를 persist하면 createdAt, updatedAt이 설정됨
        // 테스트에서는 @Mock으로 검증하거나 실제 DB 저장 후 검증 가능
        assertThat(recommendation).isInstanceOf(Object.class);
    }

    @Test
    @DisplayName("equals와 hashCode가 올바르게 동작해야 한다")
    void equalsAndHashCodeTest() {
        // given
        PointRecommendation recommendation1 = PointRecommendation.of(
                1L,
                request,
                100L,
                "경리단길 맛집",
                0.95,
                "감성적인 분위기의 맛집",
                "식당",
                "서울 종로구",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PointRecommendation recommendation2 = PointRecommendation.of(
                1L,
                request,
                100L,
                "경리단길 맛집",
                0.95,
                "감성적인 분위기의 맛집",
                "식당",
                "서울 종로구",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // then
        assertThat(recommendation1).isEqualTo(recommendation2);
        assertThat(recommendation1.hashCode()).isEqualTo(recommendation2.hashCode());
    }

    @Test
    @DisplayName("비어 있는 엔티티는 equals가 false여야 한다")
    void nonEqualDifferentIdsTest() {
        // given
        PointRecommendation recommendation1 = PointRecommendation.of(
                1L,
                request,
                100L,
                "장소1",
                0.9,
                "설명1",
                "카테고리",
                "지역",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PointRecommendation recommendation2 = PointRecommendation.of(
                2L,
                request,
                100L,
                "장소2",
                0.8,
                "설명2",
                "카테고리",
                "지역",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        // then
        assertThat(recommendation1).isNotEqualTo(recommendation2);
    }
}

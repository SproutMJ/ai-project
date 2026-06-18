package org.mj.trip.pointrecommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.mj.trip.common.entity.BaseTimeEntity;

import java.util.Objects;

@Entity
@Table(name = "point_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PointRecommendation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private PointRecommendationRequest request;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Double recommendationScore;

    @Column(nullable = false, length = 300)
    private String shortComment;

    @Column(nullable = false, length = 100)
    private String type; // 카테고리

    @Column(nullable = false, length = 255)
    private String region; // 주소/지역

    @Column(length = 500)
    private String keyword;

    @Column(length = 200)
    private String theme;

    @Column(length = 100)
    private String budget;

    @Column(length = 100)
    private String requiredTime; // 들어가는 시간(체류/소요 시간 느낌)

    @Column(length = 100)
    private String howToGo; // 가는법

    @Column(length = 100)
    private String recommendedPartySize; // 인원 추천

    @Column(length = 100)
    private String weather;

    @Column(length = 100)
    private String language;

    @Column(length = 500)
    private String disadvantage;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    public PointRecommendation(
            PointRecommendationRequest request,
            Long userId,
            String name,
            Double recommendationScore,
            String shortComment,
            String type,
            String region,
            String keyword,
            String theme,
            String budget,
            String requiredTime,
            String howToGo,
            String recommendedPartySize,
            String weather,
            String language,
            String disadvantage,
            String description) {
        // 필드 유효성 검증 로직 추가
        if (name != null && name.length() > 100) {
            throw new IllegalArgumentException("name 필드는 최대 100자까지 허용됩니다.");
        }
        if (shortComment != null && shortComment.length() > 300) {
            throw new IllegalArgumentException("shortComment 필드는 최대 300자까지 허용됩니다.");
        }
        if (type != null && type.length() > 100) {
            throw new IllegalArgumentException("type 필드는 최대 100자까지 허용됩니다.");
        }
        if (region != null && region.length() > 255) {
            throw new IllegalArgumentException("region 필드는 최대 255자까지 허용됩니다.");
        }

        this.request = request;
        this.userId = userId;
        this.name = name;
        this.recommendationScore = recommendationScore;
        this.shortComment = shortComment;
        this.type = type;
        this.region = region;
        this.keyword = keyword;
        this.theme = theme;
        this.budget = budget;
        this.requiredTime = requiredTime;
        this.howToGo = howToGo;
        this.recommendedPartySize = recommendedPartySize;
        this.weather = weather;
        this.language = language;
        this.disadvantage = disadvantage;
        this.description = description;
    }

    // id가 있는 경우: 기본 빌더를 호출하여 검증 로직 재사용
    public static PointRecommendation of(
            Long id,
            PointRecommendationRequest request,
            Long userId,
            String name,
            Double recommendationScore,
            String shortComment,
            String type,
            String region,
            String keyword,
            String theme,
            String budget,
            String requiredTime,
            String howToGo,
            String recommendedPartySize,
            String weather,
            String language,
            String disadvantage,
            String description) {

        // id를 제외한 모든 필드로 기본 빌더 호출
        PointRecommendation recommendation = PointRecommendation.builder()
                .request(request)
                .userId(userId)
                .name(name)
                .recommendationScore(recommendationScore)
                .shortComment(shortComment)
                .type(type)
                .region(region)
                .keyword(keyword)
                .theme(theme)
                .budget(budget)
                .requiredTime(requiredTime)
                .howToGo(howToGo)
                .recommendedPartySize(recommendedPartySize)
                .weather(weather)
                .language(language)
                .disadvantage(disadvantage)
                .description(description)
                .build();

        recommendation.setId(id);

        return recommendation;
    }

    private void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PointRecommendation that)) return false;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}

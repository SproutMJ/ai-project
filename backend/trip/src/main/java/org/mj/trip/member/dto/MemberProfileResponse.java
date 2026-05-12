package org.mj.trip.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberProfileResponse {

    private Long memberId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private List<TravelStyle> travelStyles;
    private String createdAt;
    private String updatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TravelStyle {
        private Long id;
        private String name;
    }
}

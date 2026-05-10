package org.mj.trip.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MemberProfileResponse {
    private Long memberId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private List<TravelStyle> travelStyles;
    private String createdAt;
    private String updatedAt;

    @Getter
    @AllArgsConstructor
    public static class TravelStyle {
        private Long id;
        private String name;
    }
}
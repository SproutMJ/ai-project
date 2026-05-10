package org.mj.trip.member.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record SignupResponse(
        Long memberId,
        String email,
        String nickname,
        String profileImageUrl,
        List<TravelStyleDto> travelStyles,
        String status,
        LocalDateTime createdAt
) {
    public record TravelStyleDto(
            Integer id,
            String name
    ) {}

    public static SignupResponse from(org.mj.trip.member.domain.Member member, List<TravelStyleDto> travelStyles) {
        return new SignupResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImageUrl(),
                travelStyles,
                member.getStatus().name(),
                member.getCreatedAt()
        );
    }
}
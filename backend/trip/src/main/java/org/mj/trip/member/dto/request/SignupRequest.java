package org.mj.trip.member.dto.request;

import java.util.List;

public record SignupRequest(
        String signupType,
        String email,
        String password,
        String nickname,
        String profileImageUrl,
        List<Integer> travelStyleIds,
        String socialProvider,
        String socialProviderAccountId
) {
    // Validation moved to MemberService to allow for structured error responses
}

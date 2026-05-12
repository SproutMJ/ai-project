package org.mj.trip.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupResponse {

    private Long memberId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime createdAt;
}

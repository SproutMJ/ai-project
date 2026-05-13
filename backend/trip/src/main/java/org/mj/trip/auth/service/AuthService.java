package org.mj.trip.auth.service;

import lombok.RequiredArgsConstructor;
import org.mj.trip.auth.dto.LoginRequest;
import org.mj.trip.auth.dto.LoginResponse;
import org.mj.trip.auth.token.JwtTokenProvider;
import org.mj.trip.common.exception.MemberNotFoundException;
import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;
import org.mj.trip.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // 이메일로 회원 조회 (상태는 ACTIVE만 허용)
        Member member = memberRepository.findByEmailAndStatus(request.getEmail(), MemberStatus.ACTIVE)
                .orElseThrow(() -> new MemberNotFoundException("회원을 찾을 수 없습니다."));

        // 비밀번호 검증
        if (!member.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 발급
        String accessToken = jwtTokenProvider.createAccessToken(member.getMemberId());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getMemberId());
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationSeconds();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .member(LoginResponse.MemberInfo.builder()
                        .memberId(member.getMemberId())
                        .nickname(member.getNickname())
                        .build())
                .build();
    }
}

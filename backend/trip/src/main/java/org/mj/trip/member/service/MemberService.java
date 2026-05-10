package org.mj.trip.member.service;

import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.dto.request.SignupRequest;
import org.mj.trip.member.dto.response.SignupResponse;
import org.mj.trip.member.repository.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public MemberProfileResponse getMyProfile(Long memberId) {
        // TODO: 실제 구현 시 Repository에서 Member 조회 후 매핑
        return new MemberProfileResponse(
                memberId,
                "user@example.com",
                "minjun",
                "https://example.com/profile.jpg",
                List.of(new MemberProfileResponse.TravelStyle(1L, "맛집 중심")),
                "2026-04-21T10:00:00Z",
                "2026-04-21T10:00:00Z"
        );
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (request.nickname() == null || request.nickname().isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        if (memberRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Member member = Member.builder()
                .email(request.email())
                .nickname(request.nickname())
                .profileImageUrl(request.profileImageUrl())
                .status(MemberStatus.ACTIVE)
                .build();

        Member savedMember = memberRepository.save(member);

        List<SignupResponse.TravelStyleDto> travelStyles = request.travelStyleIds().stream()
                .map(id -> new SignupResponse.TravelStyleDto(id, "스타일" + id))
                .toList();

        return SignupResponse.from(savedMember, travelStyles);
    }
}
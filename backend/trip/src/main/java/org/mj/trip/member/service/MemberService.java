package org.mj.trip.member.service;

import lombok.RequiredArgsConstructor;
import org.mj.trip.common.exception.DuplicateResourceException;
import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberTravelStyle;
import org.mj.trip.member.domain.TravelStyle;
import org.mj.trip.member.dto.MemberProfileResponse;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.dto.UpdateProfileRequest;
import org.mj.trip.member.dto.WithdrawRequest;
import org.mj.trip.member.repository.MemberRepository;
import org.mj.trip.member.repository.MemberTravelStyleRepository;
import org.mj.trip.member.repository.TravelStyleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberTravelStyleRepository memberTravelStyleRepository;
    private final TravelStyleRepository travelStyleRepository;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("email", "이미 사용 중인 이메일입니다.");
        }

        // Member 생성
        Member member = Member.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .nickname(request.getNickname())
                .profileImageUrl(request.getProfileImageUrl())
                .build();

        member = memberRepository.save(member);

        return SignupResponse.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .createdAt(member.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public MemberProfileResponse getMyProfile(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        List<MemberProfileResponse.TravelStyle> travelStyles = getMemberTravelStyles(memberId);

        return MemberProfileResponse.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .profileImageUrl(member.getProfileImageUrl())
                .travelStyles(travelStyles)
                .createdAt(formatDate(member.getCreatedAt()))
                .updatedAt(formatDate(member.getUpdatedAt()))
                .build();
    }

    @Transactional
    public MemberProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        // 프로필 정보 업데이트
        member.updateProfile(request.getNickname(), request.getProfileImageUrl());

        // 여행 스타일 업데이트
        if (request.getTravelStyleIds() != null) {
            updateMemberTravelStyles(memberId, request.getTravelStyleIds());
        }

        // 업데이트 후 최신 정보 조회
        Member updatedMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        List<MemberProfileResponse.TravelStyle> travelStyles = getMemberTravelStyles(memberId);

        return MemberProfileResponse.builder()
                .memberId(updatedMember.getMemberId())
                .nickname(updatedMember.getNickname())
                .profileImageUrl(updatedMember.getProfileImageUrl())
                .travelStyles(travelStyles)
                .updatedAt(formatDate(updatedMember.getUpdatedAt()))
                .build();
    }

    private List<MemberProfileResponse.TravelStyle> getMemberTravelStyles(Long memberId) {
        return memberTravelStyleRepository.findByMemberId(memberId)
                .stream()
                .map(mts -> {
                    TravelStyle style = travelStyleRepository.findById(mts.getTravelStyleId())
                            .orElseThrow(() -> new IllegalArgumentException("여행 스타일을 찾을 수 없습니다"));
                    return new MemberProfileResponse.TravelStyle(style.getId(), style.getName());
                })
                .collect(Collectors.toList());
    }

    private void updateMemberTravelStyles(Long memberId, List<Long> travelStyleIds) {
        // 기존 여행 스타일 삭제
        memberTravelStyleRepository.deleteByMemberId(memberId);

        // 새로운 여행 스타일 저장
        List<MemberTravelStyle> newStyles = travelStyleIds.stream()
                .map(styleId -> {
                    travelStyleRepository.findById(styleId)
                            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 여행 스타일입니다: " + styleId));
                    return new MemberTravelStyle(memberId, styleId);
                })
                .collect(Collectors.toList());

        memberTravelStyleRepository.saveAll(newStyles);
    }

    @Transactional
    public void withdraw(Long memberId, WithdrawRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new org.mj.trip.common.exception.MemberNotFoundException("회원을 찾을 수 없습니다"));

        member.withdraw();
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}

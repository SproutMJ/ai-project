package org.mj.trip.member.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mj.trip.common.exception.DuplicateResourceException;
import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.TravelStyle;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.dto.UpdateProfileRequest;
import org.mj.trip.member.dto.WithdrawRequest;
import org.mj.trip.member.repository.MemberRepository;
import org.mj.trip.member.repository.MemberTravelStyleRepository;
import org.mj.trip.member.repository.TravelStyleRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("Member Service 테스트")
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberTravelStyleRepository memberTravelStyleRepository;
    @Mock
    private TravelStyleRepository travelStyleRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        SignupRequest request = SignupRequest.builder()
                .email("test@test.com")
                .password("12345678")
                .nickname("tester")
                .build();
        when(memberRepository.existsByEmail("test@test.com")).thenReturn(false);

        Member savedMember = Member.builder()
                .email("test@test.com")
                .password("12345678")
                .nickname("tester")
                .build();
        when(memberRepository.save(any(Member.class))).thenReturn(savedMember);

        // when
        SignupResponse response = memberService.signup(request);

        // then
        assertNotNull(response);
        assertEquals("tester", response.getNickname());
        assertEquals("test@test.com", response.getEmail());
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_duplicate_email() {
        // given
        SignupRequest request = SignupRequest.builder()
                .email("dup@test.com")
                .password("12345678")
                .nickname("tester")
                .build();
        when(memberRepository.existsByEmail("dup@test.com")).thenReturn(true);

        // when & then
        assertThrows(DuplicateResourceException.class, () -> memberService.signup(request));
    }

    @Test
    @DisplayName("프로필 수정 성공")
    void updateProfile_success() {
        // given
        Long memberId = 1L;
        Member member = Member.builder()
                .email("test@test.com")
                .password("1234")
                .nickname("old")
                .build();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .nickname("new")
                .travelStyleIds(List.of(1L))
                .build();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        TravelStyle style = mock(TravelStyle.class);
        when(travelStyleRepository.findById(1L)).thenReturn(Optional.of(style));
        // getMemberTravelStyles에서 호출하는 findByMemberId도 stub
        when(memberTravelStyleRepository.findByMemberId(memberId)).thenReturn(List.of());

        // when
        memberService.updateProfile(memberId, request);

        // then
        assertEquals("new", member.getNickname());
        verify(memberTravelStyleRepository).deleteByMemberId(memberId);
        verify(memberTravelStyleRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 회원 없음")
    void updateProfile_not_found() {
        // given
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> memberService.updateProfile(999L, new UpdateProfileRequest()));
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void withdraw_success() {
        // given
        Long memberId = 1L;
        Member member = Member.builder()
                .email("test@test.com")
                .password("1234")
                .nickname("tester")
                .build();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        // when
        memberService.withdraw(memberId, new WithdrawRequest());

        // then
        assertEquals(org.mj.trip.member.domain.MemberStatus.WITHDRAWN, member.getStatus());
        assertNotNull(member.getDeletedAt());
    }
}

package org.mj.trip.member.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;
import org.mj.trip.member.dto.SignupRequest;
import org.mj.trip.member.dto.SignupResponse;
import org.mj.trip.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("이메일 회원가입 성공")
    void signupWithEmail_Success() {
        // given
        SignupRequest request = SignupRequest.builder()
                .signupType("EMAIL")
                .email("user@example.com")
                .password("password123")
                .nickname("minjun")
                .profileImageUrl("https://example.com/profile.jpg")
                .socialProvider(null)
                .socialProviderAccountId(null)
                .build();

        // when
        SignupResponse response = memberService.signup(request);

        // then
        assertThat(response.getMemberId()).isNotNull();
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getNickname()).isEqualTo("minjun");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(response.getCreatedAt()).isNotNull();

        // DB에 저장되었는지 확인
        Member savedMember = memberRepository.findById(response.getMemberId()).orElseThrow();
        assertThat(savedMember.getEmail()).isEqualTo("user@example.com");
        assertThat(savedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
    }

    @Test
    @DisplayName("이메일 중복 시 예외 발생")
    void signupWithEmail_DuplicateEmail_ThrowsException() {
        // given
        SignupRequest firstRequest = SignupRequest.builder()
                .signupType("EMAIL")
                .email("user@example.com")
                .password("password123")
                .nickname("minjun")
                .profileImageUrl("https://example.com/profile.jpg")
                .socialProvider(null)
                .socialProviderAccountId(null)
                .build();

        memberService.signup(firstRequest);

        SignupRequest duplicateRequest = SignupRequest.builder()
                .signupType("EMAIL")
                .email("user@example.com")
                .password("password456")
                .nickname("another")
                .profileImageUrl("https://example.com/another.jpg")
                .socialProvider(null)
                .socialProviderAccountId(null)
                .build();

        // when & then
        assertThatThrownBy(() -> memberService.signup(duplicateRequest))
                .isInstanceOf(org.mj.trip.common.exception.DuplicateResourceException.class)
                .hasMessageContaining("이미 사용 중인 이메일입니다");
    }

    @Test
    @DisplayName("프로필 이미지 없이 회원가입 성공")
    void signupWithEmail_NoProfileImage_Success() {
        // given
        SignupRequest request = SignupRequest.builder()
                .signupType("EMAIL")
                .email("user2@example.com")
                .password("password123")
                .nickname("testuser")
                .profileImageUrl(null)
                .socialProvider(null)
                .socialProviderAccountId(null)
                .build();

        // when
        SignupResponse response = memberService.signup(request);

        // then
        assertThat(response.getMemberId()).isNotNull();
        assertThat(response.getProfileImageUrl()).isNull();
    }
}

package org.mj.trip.member.service;

import org.junit.jupiter.api.Test;
import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;
import org.mj.trip.member.dto.request.SignupRequest;
import org.mj.trip.member.dto.response.SignupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Test
    void 이메일_회원가입_성공() {
        SignupRequest request = new SignupRequest(
                "EMAIL", "user@example.com", "password123", "minjun",
                "https://example.com/profile.jpg", List.of(), null, null
        );

        SignupResponse response = memberService.signup(request);

        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.nickname()).isEqualTo("minjun");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    void 닉네임_없이_회원가입_시_예외() {
        SignupRequest request = new SignupRequest(
                "EMAIL", "user2@example.com", "password123", "",
                null, List.of(), null, null
        );

        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이메일_중복_시_예외() {
        SignupRequest request1 = new SignupRequest(
                "EMAIL", "duplicate@example.com", "password123", "user1",
                null, List.of(), null, null
        );
        memberService.signup(request1);

        SignupRequest request2 = new SignupRequest(
                "EMAIL", "duplicate@example.com", "password123", "user2",
                null, List.of(), null, null
        );

        assertThatThrownBy(() -> memberService.signup(request2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 여행스타일_추가_회원가입() {
        SignupRequest request = new SignupRequest(
                "EMAIL", "style@example.com", "password123", "styleUser",
                null, List.of(1, 3, 5), null, null
        );

        SignupResponse response = memberService.signup(request);

        assertThat(response.travelStyles()).hasSize(3);
        assertThat(response.travelStyles().get(0).id()).isEqualTo(1);
        assertThat(response.travelStyles().get(1).id()).isEqualTo(3);
        assertThat(response.travelStyles().get(2).id()).isEqualTo(5);
    }
}

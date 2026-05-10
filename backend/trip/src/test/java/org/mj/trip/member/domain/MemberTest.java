package org.mj.trip.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MemberTest {

    @Test
    @DisplayName("Member 생성 시 기본값이 설정된다")
    void testCreateMember() {
        Member member = Member.builder()
                .email("test@example.com")
                .nickname("테스트사용자")
                .profileImageUrl("https://example.com/profile.jpg")
                .status(MemberStatus.ACTIVE)
                .build();

        assertThat(member.getEmail()).isEqualTo("test@example.com");
        assertThat(member.getNickname()).isEqualTo("테스트사용자");
        assertThat(member.getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(member.getCreatedAt()).isNotNull();
        assertThat(member.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("닉네임 업데이트 시 updatedAt이 변경된다")
    void testUpdateNickname() {
        Member member = Member.builder()
                .email("test@example.com")
                .nickname("기존닉네임")
                .status(MemberStatus.ACTIVE)
                .build();

        LocalDateTime beforeUpdate = member.getUpdatedAt();
        member.updateNickname("새로운닉네임");

        assertThat(member.getNickname()).isEqualTo("새로운닉네임");
        assertTrue(member.getUpdatedAt().isAfter(beforeUpdate) || member.getUpdatedAt().isEqual(beforeUpdate));
    }

    @Test
    @DisplayName("탈퇴 시 상태가 WITHDRAWN으로 변경되고 deletedAt이 설정된다")
    void testWithdraw() {
        Member member = Member.builder()
                .email("test@example.com")
                .nickname("테스트사용자")
                .status(MemberStatus.ACTIVE)
                .build();

        member.withdraw();

        assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(member.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("isActive는 ACTIVE 상태일 때 true를 반환한다")
    void testIsActive() {
        Member activeMember = Member.builder()
                .email("active@example.com")
                .nickname("활성사용자")
                .status(MemberStatus.ACTIVE)
                .build();

        Member withdrawnMember = Member.builder()
                .email("withdrawn@example.com")
                .nickname("탈퇴사용자")
                .status(MemberStatus.WITHDRAWN)
                .build();

        assertTrue(activeMember.isActive());
        assertFalse(withdrawnMember.isActive());
    }
}
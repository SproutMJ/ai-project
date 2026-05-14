package org.mj.trip.member.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Member Entity 테스트")
class MemberTest {

    @Test
    @DisplayName("회원 생성 시 기본값 설정 확인")
    void createMember() {
        // given & when
        Member member = Member.builder()
                .email("test@test.com")
                .password("password123")
                .nickname("tester")
                .build();

        // then
        assertEquals(MemberStatus.ACTIVE, member.getStatus());
        assertNotNull(member.getCreatedAt());
        assertNull(member.getDeletedAt());
        assertTrue(member.isActive());
    }

    @Test
    @DisplayName("프로필 수정")
    void updateProfile() {
        // given
        Member member = Member.builder().email("test@test.com").password("1234").nickname("old").build();

        // when
        member.updateProfile("new_nickname", "http://new.com/image.jpg");

        // then
        assertEquals("new_nickname", member.getNickname());
        assertEquals("http://new.com/image.jpg", member.getProfileImageUrl());
        assertNotNull(member.getUpdatedAt());
    }

    @Test
    @DisplayName("회원 탈퇴 시 상태 및 삭제 시간 변경")
    void withdraw() {
        // given
        Member member = Member.builder().email("test@test.com").password("1234").nickname("tester").build();
        LocalDateTime before = member.getCreatedAt();

        // when
        member.withdraw();

        // then
        assertEquals(MemberStatus.WITHDRAWN, member.getStatus());
        assertNotNull(member.getDeletedAt());
        assertFalse(member.isActive());
    }
}

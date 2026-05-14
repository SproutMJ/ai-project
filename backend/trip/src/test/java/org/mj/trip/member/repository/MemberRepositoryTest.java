package org.mj.trip.member.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mj.trip.member.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Member Repository 테스트")
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 저장 및 ID 조회")
    void saveAndFindById() {
        // given
        Member member = Member.builder()
                .email("test@test.com")
                .password("password123")
                .nickname("tester")
                .build();

        // when
        Member savedMember = memberRepository.save(member);

        // then
        assertNotNull(savedMember.getMemberId());
        assertEquals("test@test.com", savedMember.getEmail());
    }

    @Test
    @DisplayName("이메일로 회원 조회")
    void findByEmail() {
        // given
        memberRepository.save(Member.builder().email("find@test.com").password("1234").nickname("find").build());

        // when
        Optional<Member> found = memberRepository.findByEmail("find@test.com");

        // then
        assertTrue(found.isPresent());
        assertEquals("find", found.get().getNickname());
    }

    @Test
    @DisplayName("이메일 중복 확인")
    void existsByEmail() {
        // given
        memberRepository.save(Member.builder().email("exist@test.com").password("1234").nickname("exist").build());

        // when & then
        assertTrue(memberRepository.existsByEmail("exist@test.com"));
        assertFalse(memberRepository.existsByEmail("noexist@test.com"));
    }

    @Test
    @DisplayName("닉네임 키워드로 검색")
    void searchByNickname() {
        // given
        memberRepository.save(Member.builder().email("a@a.com").password("1234").nickname("KimMinJun").build());
        memberRepository.save(Member.builder().email("b@b.com").password("1234").nickname("ParkMinJun").build());
        memberRepository.save(Member.builder().email("c@c.com").password("1234").nickname("LeeSooHyun").build());

        // when
        List<Member> result = memberRepository.searchByNickname("MinJun");

        // then
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("활성 상태 회원 목록 조회")
    void findActiveMembersByIds() {
        // given
        Long id1 = memberRepository.save(Member.builder().email("1@1.com").password("1234").nickname("Active1").build()).getMemberId();
        Long id2 = memberRepository.save(Member.builder().email("2@2.com").password("1234").nickname("Active2").build()).getMemberId();
        Member withdrawn = Member.builder().email("3@3.com").password("1234").nickname("Withdrawn").build();
        withdrawn.withdraw(); // 상태 변경
        memberRepository.save(withdrawn);
        Long id3 = withdrawn.getMemberId();

        // when
        List<Member> result = memberRepository.findActiveMembersByIds(List.of(id1, id2, id3));

        // then
        assertEquals(2, result.size()); // 탈퇴한 회원은 제외
    }
}

package org.mj.trip.member.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Member를 저장하고 조회한다")
    void testSaveAndFindMember() {
        Member member = Member.builder()
                .email("test@example.com")
                .nickname("테스트사용자")
                .profileImageUrl("https://example.com/profile.jpg")
                .status(MemberStatus.ACTIVE)
                .build();

        Member savedMember = memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        Optional<Member> foundMember = memberRepository.findById(savedMember.getMemberId());
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundMember.get().getNickname()).isEqualTo("테스트사용자");
    }

    @Test
    @DisplayName("이메일로 Member를 조회한다")
    void testFindByEmail() {
        Member member = Member.builder()
                .email("unique@example.com")
                .nickname("이메일테스트")
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        Optional<Member> foundMember = memberRepository.findByEmail("unique@example.com");
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo("unique@example.com");
    }

    @Test
    @DisplayName("이메일과 상태로 Member를 조회한다")
    void testFindByEmailAndStatus() {
        Member member = Member.builder()
                .email("status@example.com")
                .nickname("상태테스트")
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        Optional<Member> foundMember = memberRepository.findByEmailAndStatus("status@example.com", MemberStatus.ACTIVE);
        assertThat(foundMember).isPresent();

        Optional<Member> notFound = memberRepository.findByEmailAndStatus("status@example.com", MemberStatus.WITHDRAWN);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("이메일이 중복인지 확인한다")
    void testExistsByEmail() {
        Member member = Member.builder()
                .email("duplicate@example.com")
                .nickname("중복테스트")
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        assertThat(memberRepository.existsByEmail("duplicate@example.com")).isTrue();
        assertThat(memberRepository.existsByEmail("nonexistent@example.com")).isFalse();
    }

    @Test
    @DisplayName("닉네임으로 회원을 검색한다")
    void testSearchByNickname() {
        Member member1 = Member.builder()
                .email("search1@example.com")
                .nickname("테스트닉네임1")
                .status(MemberStatus.ACTIVE)
                .build();

        Member member2 = Member.builder()
                .email("search2@example.com")
                .nickname("테스트닉네임2")
                .status(MemberStatus.ACTIVE)
                .build();

        memberRepository.save(member1);
        memberRepository.save(member2);
        entityManager.flush();
        entityManager.clear();

        List<Member> results = memberRepository.searchByNickname("테스트");
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Active 상태의 회원만 조회된다")
    void testFindActiveMembersByIds() {
        Member activeMember = Member.builder()
                .email("active1@example.com")
                .nickname("활성회원")
                .status(MemberStatus.ACTIVE)
                .build();

        Member withdrawnMember = Member.builder()
                .email("withdrawn@example.com")
                .nickname("탈퇴회원")
                .status(MemberStatus.WITHDRAWN)
                .build();

        Member savedActive = memberRepository.save(activeMember);
        memberRepository.save(withdrawnMember);
        entityManager.flush();
        entityManager.clear();

        List<Member> results = memberRepository.findActiveMembersByIds(List.of(savedActive.getMemberId(), withdrawnMember.getMemberId()));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("active1@example.com");
    }
}
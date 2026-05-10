package org.mj.trip.member.repository;

import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    boolean existsByEmail(String email);

    @Query("SELECT m FROM Member m WHERE m.nickname LIKE %:keyword% AND m.status = :status")
    List<Member> searchByNickname(String keyword, MemberStatus status);

    default List<Member> searchByNickname(String keyword) {
        return searchByNickname(keyword, MemberStatus.ACTIVE);
    }

    @Query("SELECT m FROM Member m WHERE m.memberId IN :memberIds AND m.status = MemberStatus.ACTIVE")
    List<Member> findActiveMembersByIds(List<Long> memberIds);
}
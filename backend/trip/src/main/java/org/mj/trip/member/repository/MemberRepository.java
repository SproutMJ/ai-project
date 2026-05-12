package org.mj.trip.member.repository;

import org.mj.trip.member.domain.Member;
import org.mj.trip.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Member> findByEmailAndStatus(String email, MemberStatus status);

    @Query("SELECT m FROM Member m WHERE m.nickname LIKE %:keyword%")
    List<Member> searchByNickname(@Param("keyword") String keyword);

    @Query("SELECT m FROM Member m WHERE m.memberId IN :memberIds AND m.status = MemberStatus.ACTIVE")
    List<Member> findActiveMembersByIds(@Param("memberIds") List<Long> memberIds);
}

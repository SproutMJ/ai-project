package org.mj.trip.member.repository;

import org.mj.trip.member.domain.MemberTravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberTravelStyleRepository extends JpaRepository<MemberTravelStyle, Long> {

    List<MemberTravelStyle> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}

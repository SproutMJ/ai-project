package org.mj.trip.member.repository;

import org.mj.trip.member.domain.TravelStyle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TravelStyleRepository extends JpaRepository<TravelStyle, Long> {

    Optional<TravelStyle> findByName(String name);

    List<TravelStyle> findByIdIn(List<Long> ids);
}

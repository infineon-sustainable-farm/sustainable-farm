package com.infineonbit.sustainablefarm.modules.visitormanagement.repository;

import com.infineonbit.sustainablefarm.modules.visitormanagement.entity.TourStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourStopRepository extends JpaRepository<TourStop, Long> {

    List<TourStop> findAllByOrderByPositionAsc();

    boolean existsByActiveTrueAndPosition(int position);

    boolean existsByActiveTrueAndPositionAndIdNot(int position, Long id);
}
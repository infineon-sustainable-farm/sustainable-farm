package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterLevelHistory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterLevelHistoryRepository extends JpaRepository<WaterLevelHistory, Long> {
    List<WaterLevelHistory> findBySourceIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            Long sourceId, Instant from, Instant to);
}

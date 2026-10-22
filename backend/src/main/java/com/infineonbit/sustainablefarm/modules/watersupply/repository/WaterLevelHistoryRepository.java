package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterLevelHistory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterLevelHistoryRepository extends JpaRepository<WaterLevelHistory, UUID> {
    List<WaterLevelHistory> findBySourceIdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
            UUID sourceId, Instant from, Instant to);
}

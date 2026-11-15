package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.SoilMoistureReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SoilMoistureReadingRepository extends JpaRepository<SoilMoistureReading, UUID> {

    List<SoilMoistureReading> findByZoneIdOrderByMeasuredAtDesc(UUID zoneId);

    @Query("select sr from SoilMoistureReading sr where sr.zoneId = :zoneId and sr.measuredAt <= :at order by sr.measuredAt desc")
    List<SoilMoistureReading> findLatestBefore(UUID zoneId, Instant at);

    @Query("select sr.moisturePercent from SoilMoistureReading sr where sr.zoneId = :zoneId and sr.measuredAt <= :at order by sr.measuredAt desc")
    Double findLatestMoisturePercentBefore(UUID zoneId, Instant at);
}

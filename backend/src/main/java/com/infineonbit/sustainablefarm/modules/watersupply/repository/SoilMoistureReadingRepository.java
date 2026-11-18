package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.SoilMoistureReading;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SoilMoistureReadingRepository extends JpaRepository<SoilMoistureReading, Long> {

    List<SoilMoistureReading> findByZoneIdOrderByMeasuredAtDesc(Long zoneId);

    @Query("select sr from SoilMoistureReading sr where sr.zoneId = :zoneId and sr.measuredAt <= :at order by sr.measuredAt desc")
    List<SoilMoistureReading> findLatestBefore(Long zoneId, Instant at);

    @Query("select sr.moisturePercent from SoilMoistureReading sr where sr.zoneId = :zoneId and sr.measuredAt <= :at order by sr.measuredAt desc")
    Double findLatestMoisturePercentBefore(Long zoneId, Instant at);
}

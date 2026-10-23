package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WaterConsumptionRepository extends JpaRepository<WaterConsumption, UUID> {
    Page<WaterConsumption> findByFarmId(UUID farmId, Pageable pageable);

    Page<WaterConsumption> findBySourceId(UUID sourceId, Pageable pageable);

    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w where w.consumptionDate >= :start")
    double sumConsumptionSince(Instant start);

    /** Cumulated consumption over a semi-open interval [start, end[ (day-by-day series). */
    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w "
            + "where w.consumptionDate >= :start and w.consumptionDate < :end")
    double sumConsumptionBetween(Instant start, Instant end);

    /**
     * Cumulated consumption of a farm over a semi-open interval [start, end[:
     * supports the monthly per-farm quota tracking (P8).
     */
    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w "
            + "where w.consumptionDate >= :start and w.consumptionDate < :end and w.farmId = :farmId")
    double sumConsumptionByFarmIdBetween(Instant start, Instant end, UUID farmId);

    /**
     * Volume measured on a zone over a semi-open interval [start, end[: this is the
     * "actual flow" of the clogging detection (see DripFlowCheckService), compared to the
     * theoretical volume of the drip network over the same watering duration.
     */
    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w "
            + "where w.zoneId = :zoneId and w.consumptionDate >= :start and w.consumptionDate < :end")
    double sumConsumptionByZoneIdBetween(Instant start, Instant end, UUID zoneId);
}
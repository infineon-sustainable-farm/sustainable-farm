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

    /** Consommation cumulée sur un intervalle semi-ouvert [start, end[ (séries jour par jour). */
    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w "
            + "where w.consumptionDate >= :start and w.consumptionDate < :end")
    double sumConsumptionBetween(Instant start, Instant end);

    /**
     * Consommation cumulée d'une ferme sur un intervalle semi-ouvert [start, end[ :
     * support du suivi des quotas mensuels par ferme (P8).
     */
    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w "
            + "where w.consumptionDate >= :start and w.consumptionDate < :end and w.farmId = :farmId")
    double sumConsumptionByFarmIdBetween(Instant start, Instant end, UUID farmId);
}
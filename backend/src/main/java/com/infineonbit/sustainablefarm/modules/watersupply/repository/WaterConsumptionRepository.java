package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WaterConsumptionRepository extends JpaRepository<WaterConsumption, UUID> {
    @Query("select coalesce(sum(w.consumptionLiters), 0) from WaterConsumption w where w.consumptionDate >= :start")
    double sumConsumptionSince(Instant start);
}
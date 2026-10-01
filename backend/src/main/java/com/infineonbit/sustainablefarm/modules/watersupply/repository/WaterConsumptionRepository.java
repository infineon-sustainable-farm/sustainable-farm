package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterConsumption;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterConsumptionRepository extends JpaRepository<WaterConsumption, UUID> {
    Page<WaterConsumption> findByFarmId(UUID farmId, Pageable pageable);

    Page<WaterConsumption> findBySourceId(UUID sourceId, Pageable pageable);
}
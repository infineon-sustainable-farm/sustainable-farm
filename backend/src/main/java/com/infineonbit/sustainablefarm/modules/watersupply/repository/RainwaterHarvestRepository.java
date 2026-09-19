package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RainwaterHarvestRepository extends JpaRepository<RainwaterHarvest, UUID> {
}

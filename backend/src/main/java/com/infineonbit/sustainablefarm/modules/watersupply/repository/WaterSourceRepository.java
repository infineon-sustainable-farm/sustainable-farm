package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterSourceRepository extends JpaRepository<WaterSource, UUID> {
	Page<WaterSource> findByFarmId(UUID farmId, Pageable pageable);
}
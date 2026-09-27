package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WaterSourceRepository extends JpaRepository<WaterSource, UUID> {
	Page<WaterSource> findByFarmId(UUID farmId, Pageable pageable);

	/** Capacite totale des reservoirs, calculee par la base (KPI de niveau du dashboard). */
	@Query("select coalesce(sum(s.capacityLiters), 0) from WaterSource s")
	double sumCapacityLiters();

	/** Niveau total actuel des reservoirs, calcule par la base (KPI de niveau du dashboard). */
	@Query("select coalesce(sum(s.currentLevelLiters), 0) from WaterSource s")
	double sumCurrentLevelLiters();
}
package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSource;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WaterSourceRepository extends JpaRepository<WaterSource, Long> {
	Page<WaterSource> findByFarmId(Long farmId, Pageable pageable);

	List<WaterSource> findByFarmIdIn(Collection<Long> farmIds);

	/** Total capacity of the reservoirs, computed in the database (dashboard level KPI). */
	@Query("select coalesce(sum(s.capacityLiters), 0) from WaterSource s")
	double sumCapacityLiters();

	/** Current total level of the reservoirs, computed in the database (dashboard level KPI). */
	@Query("select coalesce(sum(s.currentLevelLiters), 0) from WaterSource s")
	double sumCurrentLevelLiters();
}
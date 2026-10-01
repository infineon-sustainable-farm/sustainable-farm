package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.CarbonMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CarbonMetricRepository extends JpaRepository<CarbonMetric, String> {
}

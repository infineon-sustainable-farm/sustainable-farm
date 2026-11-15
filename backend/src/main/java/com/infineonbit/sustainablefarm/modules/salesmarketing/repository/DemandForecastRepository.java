package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.DemandForecast;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandForecastRepository extends JpaRepository<DemandForecast, Integer> {
    // Basic CRUD is provided by JpaRepository.
}

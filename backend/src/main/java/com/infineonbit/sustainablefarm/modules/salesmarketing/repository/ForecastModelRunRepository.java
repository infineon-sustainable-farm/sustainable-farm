package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ForecastModelRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForecastModelRunRepository extends JpaRepository<ForecastModelRun, Integer> {
    // Basic CRUD is provided by JpaRepository.
}

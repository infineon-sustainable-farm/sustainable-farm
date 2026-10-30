package com.infineonbit.sustainablefarm.modules.energysupply.repository;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SolarGenerationLogRepository extends JpaRepository<SolarGenerationLog, String> {

    List<SolarGenerationLog> findByDateBetween(LocalDate start, LocalDate end);
}

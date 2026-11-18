package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DripMaintenanceLogRepository extends JpaRepository<DripMaintenanceLog, Long> {
    Page<DripMaintenanceLog> findByZoneId(Long zoneId, Pageable pageable);
}

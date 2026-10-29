package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DripMaintenanceLogRepository extends JpaRepository<DripMaintenanceLog, UUID> {
    Page<DripMaintenanceLog> findByZoneId(UUID zoneId, Pageable pageable);
}

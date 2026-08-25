package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.DripMaintenanceLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DripMaintenanceLogRepository extends JpaRepository<DripMaintenanceLog, UUID> {
}

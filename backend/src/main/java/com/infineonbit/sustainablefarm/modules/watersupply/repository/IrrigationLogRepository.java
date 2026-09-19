package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IrrigationLogRepository extends JpaRepository<IrrigationLog, UUID> {
	Page<IrrigationLog> findByScheduleId(UUID scheduleId, Pageable pageable);

	Optional<IrrigationLog> findFirstByScheduleIdAndStatusOrderByActualStartTimeDesc(UUID scheduleId, String status);
}

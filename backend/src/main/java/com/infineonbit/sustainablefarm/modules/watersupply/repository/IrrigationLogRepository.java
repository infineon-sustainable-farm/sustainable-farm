package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IrrigationLogRepository extends JpaRepository<IrrigationLog, UUID> {
	Page<IrrigationLog> findByScheduleId(UUID scheduleId, Pageable pageable);

	Optional<IrrigationLog> findFirstByScheduleIdAndStatusOrderByActualStartTimeDesc(UUID scheduleId, String status);

	/**
	 * Irrigation cycles of a zone started within the interval [start, end[: the zone is read
	 * from the schedule (irrigation_logs only stores the schedule). Used for clogging detection
	 * (actual irrigation duration of the zone) and the "last irrigation > 24 h" rule.
	 */
	@Query("select l from IrrigationLog l, IrrigationSchedule s "
			+ "where l.scheduleId = s.id and s.zoneId = :zoneId "
			+ "and l.actualStartTime >= :start and l.actualStartTime < :end")
	List<IrrigationLog> findForZoneBetween(UUID zoneId, Instant start, Instant end);
}

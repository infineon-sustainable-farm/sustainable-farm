package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLog;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationLogStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IrrigationLogRepository extends JpaRepository<IrrigationLog, Long> {
	Page<IrrigationLog> findByScheduleId(Long scheduleId, Pageable pageable);

	Optional<IrrigationLog> findFirstByScheduleIdAndStatusOrderByActualStartTimeDesc(Long scheduleId, IrrigationLogStatus status);

    Optional<IrrigationLog> findFirstByScheduleIdOrderByActualStartTimeDesc(Long scheduleId);

	/**
	 * Irrigation cycles of a zone started within the interval [start, end[: the zone is read
	 * from the schedule (irrigation_logs only stores the schedule). Used for clogging detection
	 * (actual irrigation duration of the zone) and the "last irrigation > 24 h" rule.
	 */
	@Query("select l from IrrigationLog l, IrrigationSchedule s "
			+ "where l.scheduleId = s.id and s.zoneId = :zoneId "
			+ "and l.actualStartTime >= :start and l.actualStartTime < :end")
	List<IrrigationLog> findForZoneBetween(Long zoneId, Instant start, Instant end);
}

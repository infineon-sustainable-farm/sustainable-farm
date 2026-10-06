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
	 * Cycles d'arrosage d'une zone demarres dans l'intervalle [start, end[ : la zone se lit
	 * par le planning (irrigation_logs ne stocke que le planning). Sert a la detection de
	 * colmatage (duree d'arrosage reelle de la zone) et a la regle « dernier arrosage > 24 h ».
	 */
	@Query("select l from IrrigationLog l, IrrigationSchedule s "
			+ "where l.scheduleId = s.id and s.zoneId = :zoneId "
			+ "and l.actualStartTime >= :start and l.actualStartTime < :end")
	List<IrrigationLog> findForZoneBetween(UUID zoneId, Instant start, Instant end);
}

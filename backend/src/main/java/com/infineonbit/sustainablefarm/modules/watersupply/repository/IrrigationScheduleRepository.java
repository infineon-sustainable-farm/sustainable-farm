package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.IrrigationSchedule;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IrrigationScheduleRepository extends JpaRepository<IrrigationSchedule, UUID> {
    long countByStartTimeBetween(Instant start, Instant end);
}

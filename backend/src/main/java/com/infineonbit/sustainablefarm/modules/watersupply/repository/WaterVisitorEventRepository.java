package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorEvent;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterVisitorEventRepository extends JpaRepository<WaterVisitorEvent, Long> {
    Optional<WaterVisitorEvent> findByVisitId(String visitId);
}

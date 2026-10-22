package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterVisitorEventRepository extends JpaRepository<WaterVisitorEvent, UUID> {
    Optional<WaterVisitorEvent> findByVisitId(String visitId);
}

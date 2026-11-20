package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterSafetyBriefing;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterSafetyBriefingRepository extends JpaRepository<WaterSafetyBriefing, Long> {
    Optional<WaterSafetyBriefing> findFirstByOrderByCreatedAtDesc();
}

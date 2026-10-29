package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterVisitorStop;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterVisitorStopRepository extends JpaRepository<WaterVisitorStop, UUID> {
    /** Visitor Management sends stop names as it displays them: the lookup ignores case. */
    Optional<WaterVisitorStop> findByStopNameIgnoreCase(String stopName);

    List<WaterVisitorStop> findAllByOrderByStopNameAsc();
}

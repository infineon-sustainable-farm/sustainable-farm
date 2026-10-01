package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterQuotaRepository extends JpaRepository<WaterQuota, UUID> {

    List<WaterQuota> findByTargetTypeAndTargetIdOrderByQuotaMonthDesc(String targetType, UUID targetId);

    Optional<WaterQuota> findFirstByTargetTypeAndTargetIdAndQuotaMonthBetweenOrderByQuotaMonthDesc(
            String targetType, UUID targetId, LocalDate monthStart, LocalDate monthEnd);
}

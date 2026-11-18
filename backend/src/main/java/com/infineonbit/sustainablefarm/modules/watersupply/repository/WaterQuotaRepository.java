package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.QuotaTargetType;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQuota;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterQuotaRepository extends JpaRepository<WaterQuota, Long> {

    List<WaterQuota> findByTargetTypeAndTargetIdOrderByQuotaMonthDesc(QuotaTargetType targetType, Long targetId);

    Optional<WaterQuota> findFirstByTargetTypeAndTargetIdAndQuotaMonthBetweenOrderByQuotaMonthDesc(
            QuotaTargetType targetType, Long targetId, LocalDate monthStart, LocalDate monthEnd);
}

package com.infineonbit.sustainablefarm.modules.watersupply.repository;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.WaterQualityTest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaterQualityTestRepository extends JpaRepository<WaterQualityTest, UUID> {
	Page<WaterQualityTest> findBySourceId(UUID sourceId, Pageable pageable);
}
package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {

    List<QualityCheck> findByBatchIdOrderByCheckedAtDesc(Long batchId);
}
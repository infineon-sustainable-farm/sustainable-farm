package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.QualityCheck;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityCheckRepository extends JpaRepository<QualityCheck, Long> {

    @Override
    @EntityGraph(attributePaths = {"batch", "batch.supplier", "batch.storageZone"})
    List<QualityCheck> findAll();

    @EntityGraph(attributePaths = {"batch", "batch.supplier", "batch.storageZone"})
    List<QualityCheck> findByBatchIdOrderByCheckedAtDesc(Long batchId);
}
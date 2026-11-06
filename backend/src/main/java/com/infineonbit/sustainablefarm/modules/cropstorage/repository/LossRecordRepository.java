package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LossRecordRepository extends JpaRepository<LossRecord, Long> {

    @Override
    @EntityGraph(attributePaths = {"batch", "batch.supplier", "batch.storageZone"})
    List<LossRecord> findAll();

    @EntityGraph(attributePaths = {"batch", "batch.supplier", "batch.storageZone"})
    List<LossRecord> findByBatchIdOrderByRecordedAtDesc(Long batchId);
}
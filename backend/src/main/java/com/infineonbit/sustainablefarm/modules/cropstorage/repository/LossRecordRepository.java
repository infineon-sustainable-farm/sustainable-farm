package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LossRecordRepository extends JpaRepository<LossRecord, Long> {

    List<LossRecord> findByBatchIdOrderByRecordedAtDesc(Long batchId);
}
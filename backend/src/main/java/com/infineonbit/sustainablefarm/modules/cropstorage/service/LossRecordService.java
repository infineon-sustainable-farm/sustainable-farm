package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.LossRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LossRecordService {

    private final LossRecordRepository lossRecordRepository;

    public LossRecord create(LossRecord record) {
        return lossRecordRepository.save(record);
    }

    public List<LossRecord> findAll() {
        return lossRecordRepository.findAll();
    }

    public List<LossRecord> findByBatch(Long batchId) {
        return lossRecordRepository.findByBatchIdOrderByRecordedAtDesc(batchId);
    }
}
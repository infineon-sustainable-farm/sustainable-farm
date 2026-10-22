package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.LossRecord;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.LossRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LossRecordService {

    private final LossRecordRepository lossRecordRepository;
    private final BatchRepository batchRepository;

    /**
     * Saves a loss record; the batch arriving as a bare-id shell would be
     * read as unsaved since Batch is versioned, so the managed batch is
     * attached first, with a 404 if the id is unknown.
     */
    public LossRecord create(LossRecord record) {
        if (record.getBatch() != null && record.getBatch().getId() != null) {
            record.setBatch(batchRepository.findById(record.getBatch().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Batch not found with id " + record.getBatch().getId())));
        }
        return lossRecordRepository.save(record);
    }

    public List<LossRecord> findAll() {
        return lossRecordRepository.findAll();
    }

    public List<LossRecord> findByBatch(Long batchId) {
        return lossRecordRepository.findByBatchIdOrderByRecordedAtDesc(batchId);
    }
}
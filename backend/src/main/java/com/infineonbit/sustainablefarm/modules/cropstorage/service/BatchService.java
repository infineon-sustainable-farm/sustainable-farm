package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;

    public Batch create(Batch batch) {
        return batchRepository.save(batch);
    }

    public List<Batch> findAll() {
        return batchRepository.findAll();
    }

    // HYPOTHESIS: FIFO = batches with remaining stock, oldest storageEntryDate first
    public List<Batch> fifo() {
        return batchRepository.findByCurrentQuantityKgGreaterThanOrderByStorageEntryDateAsc(BigDecimal.ZERO);
    }
}
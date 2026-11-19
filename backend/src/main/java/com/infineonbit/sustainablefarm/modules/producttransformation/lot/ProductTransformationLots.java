package com.infineonbit.sustainablefarm.modules.producttransformation.lot;

import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLot;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLotLookup;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.ProcessingBatch;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.ProcessingBatchLookup;
import com.infineonbit.sustainablefarm.modules.producttransformation.batch.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.producttransformation.packagingrecord.repository.PackagingRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** The batches and packaged lots of product transformation, for the other modules. */
@Service
@Transactional(readOnly = true)
public class ProductTransformationLots implements PackagedLotLookup, ProcessingBatchLookup {

    private final PackagingRecordRepository packagingRecords;
    private final BatchRepository batches;

    public ProductTransformationLots(PackagingRecordRepository packagingRecords, BatchRepository batches) {
        this.packagingRecords = packagingRecords;
        this.batches = batches;
    }

    @Override
    public Optional<PackagedLot> findByLotCode(String lotCode) {
        if (lotCode == null || lotCode.isBlank()) {
            return Optional.empty();
        }
        // lot_code is unique (uk_packaging_record_lot_code): at most one record.
        return packagingRecords.findByLotCode(lotCode.trim()).stream()
                .findFirst()
                .map(record -> new PackagedLot(record.getId(), record.getRecordId(), record.getLotCode(),
                        record.getBatch() != null ? record.getBatch().getBatchId() : null));
    }

    @Override
    public Optional<ProcessingBatch> findBatch(String batchId) {
        if (batchId == null || batchId.isBlank()) {
            return Optional.empty();
        }
        return batches.findByBatchId(batchId.trim())
                .map(batch -> new ProcessingBatch(batch.getId(), batch.getBatchId(), batch.getHarvestDate(), batch.getVarietyKey()));
    }
}

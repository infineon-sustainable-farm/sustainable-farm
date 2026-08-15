package com.sustainablefarm.service.impl;

import com.sustainablefarm.model.Batch;
import com.sustainablefarm.model.Batch.BatchStatus;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.repository.BatchRepository;
import com.sustainablefarm.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service Implementation for Batch Entity
 * Central traceability entity for mango processing - Core Processing Entity
 * 
 * Business Rules:
 * - Status transitions must follow the processing workflow
 * - Status transitions: CREATED → INTAKE → WASHING → DRYING → PACKAGING → COMPLETED → SHIPPED
 * - One batch corresponds to one harvest event
 * - Central traceability entity for all processing stages
 * 
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@Service
@Transactional
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;

    @Autowired
    public BatchServiceImpl(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @Override
    public Batch createBatch(Batch batch) {
        // Business Rule: Default to CREATED status if not set
        if (batch.getCurrentStatus() == null) {
            batch.setCurrentStatus(BatchStatus.CREATED);
        }
        return batchRepository.save(batch);
    }

    @Override
    public Batch updateBatch(String batchId, Batch batch) {
        Batch existingBatch = getBatchById(batchId);
        
        // Update fields
        existingBatch.setHarvestDate(batch.getHarvestDate());
        existingBatch.setMangoVariety(batch.getMangoVariety());
        existingBatch.setHarvestQuantityKg(batch.getHarvestQuantityKg());
        existingBatch.setFarmId(batch.getFarmId());
        existingBatch.setBlockId(batch.getBlockId());
        
        // Only update status if explicitly set and valid
        if (batch.getCurrentStatus() != null) {
            existingBatch.setCurrentStatus(batch.getCurrentStatus());
        }
        
        return batchRepository.save(existingBatch);
    }

    @Override
    public void deleteBatch(String batchId) {
        if (!batchRepository.existsById(batchId)) {
            throw new IllegalArgumentException("Batch not found with ID: " + batchId);
        }
        batchRepository.deleteById(batchId);
    }

    @Override
    @Transactional(readOnly = true)
    public Batch getBatchById(String batchId) {
        return batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getAllBatches() {
        return batchRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByStatus(BatchStatus status) {
        return batchRepository.findByCurrentStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByHarvestDateRange(LocalDate startDate, LocalDate endDate) {
        return batchRepository.findByHarvestDateBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByVariety(MangoVariety variety) {
        return batchRepository.findByMangoVariety(variety);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByFarm(String farmId) {
        return batchRepository.findByFarmId(farmId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByFarmAndStatus(String farmId, BatchStatus status) {
        return batchRepository.findByFarmAndStatus(farmId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByVarietyAndStatus(MangoVariety variety, BatchStatus status) {
        return batchRepository.findByVarietyAndStatus(variety, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBatchesByStatus(BatchStatus status) {
        return batchRepository.countByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public Double getTotalHarvestQuantityByVariety(MangoVariety variety) {
        return batchRepository.getTotalHarvestQuantityByVariety(variety);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesReadyForShipping() {
        return batchRepository.findReadyForShipping();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByBlock(String blockId) {
        return batchRepository.findByBlockId(blockId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Batch> getBatchesByFarmAndBlock(String farmId, String blockId) {
        return batchRepository.findByFarmAndBlock(farmId, blockId);
    }

    @Override
    public Batch advanceBatchStatus(String batchId) {
        Batch batch = getBatchById(batchId);
        
        // Business Rule: Status transitions must follow the processing workflow
        try {
            batch.advanceStatus();
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("Cannot advance batch status: " + e.getMessage());
        }
        
        return batchRepository.save(batch);
    }

    @Override
    public Batch setBatchStatus(String batchId, BatchStatus status) {
        Batch batch = getBatchById(batchId);
        
        // Business Rule: Allow manual status setting for operations like REJECTED
        batch.setCurrentStatus(status);
        
        return batchRepository.save(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByBatchIdAndStatus(String batchId, BatchStatus status) {
        return batchRepository.existsByBatchIdAndCurrentStatus(batchId, status);
    }
}
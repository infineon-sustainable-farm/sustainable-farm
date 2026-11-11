package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API-005: receives the Product Transformation storage entry (dried mango
 * ready after packaging) and stores it as a new batch plus its traceability
 * IN movement, in one transaction. The batch is born full, so the movement
 * only records the entry; an unknown zone answers 404, a capacity overflow
 * or an already registered batch_id answers 409.
 */
@Service
@RequiredArgsConstructor
public class StorageEntryService {

    private final BatchRepository batchRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public StorageEntryResponse registerEntry(StorageEntryRequest request) {
        if (batchRepository.existsByBatchCode(request.batchId())) {
            throw new ConflictException("Storage entry already registered for batch_id " + request.batchId());
        }
        StorageZone zone = chooseZone(request);
        BigDecimal remainingBefore = remainingCapacity(zone);

        Batch batch = new Batch();
        batch.setBatchCode(request.batchId());
        batch.setProductType("DRIED_MANGO");
        batch.setProductStage("PACKAGED");
        batch.setQualityGrade(request.productGrade());
        batch.setStatus("STORED");
        batch.setStorageZone(zone);
        batch.setInitialQuantityKg(request.driedQuantityKg());
        batch.setPackagingDate(request.packagingDate().toLocalDate());
        batch.setStorageEntryDate(LocalDate.now());
        // Plants traceability: harvest_id arrives as a string; store its
        // numeric form, anything not numeric means "no Plants link".
        batch.setHarvestId(parseLongOrNull(request.harvestId()));
        Batch savedBatch = batchRepository.save(batch);

        StockMovement movement = new StockMovement();
        movement.setType("IN");
        movement.setQuantityKg(request.driedQuantityKg());
        movement.setBatch(savedBatch);
        movement.setToZone(zone);
        movement.setReason("Product Transformation storage entry, packaging " + request.packagingId());
        movement.setMovedAt(LocalDateTime.now());
        stockMovementRepository.save(movement);

        return new StorageEntryResponse(
                String.valueOf(savedBatch.getId()),
                String.valueOf(zone.getId()),
                savedBatch.getCreatedAt(),
                remainingBefore.subtract(request.driedQuantityKg()),
                savedBatch.getStatus());
    }

    /**
     * Uses the requested zone when given, otherwise picks the zone with the
     * most room left (HYPOTHESIS until the assignment rule is agreed with
     * Product Transformation).
     */
    private StorageZone chooseZone(StorageEntryRequest request) {
        if (request.targetStorageLocationId() != null && !request.targetStorageLocationId().isBlank()) {
            Long zoneId = parseLongOrNull(request.targetStorageLocationId());
            StorageZone zone = zoneId != null ? storageZoneRepository.findById(zoneId).orElse(null) : null;
            if (zone == null) {
                throw new ResourceNotFoundException(
                        "Storage location not found with id " + request.targetStorageLocationId());
            }
            if (remainingCapacity(zone).compareTo(request.driedQuantityKg()) < 0) {
                throw new ConflictException("Zone capacity exceeded");
            }
            return zone;
        }
        return storageZoneRepository.findAll().stream()
                .filter(z -> remainingCapacity(z).compareTo(request.driedQuantityKg()) >= 0)
                .max((a, b) -> remainingCapacity(a).compareTo(remainingCapacity(b)))
                .orElseThrow(() -> new ConflictException("Zone capacity exceeded"));
    }

    /** Room left in a zone: capacity minus the stock of the batches inside. */
    private BigDecimal remainingCapacity(StorageZone zone) {
        BigDecimal used = batchRepository.findByStorageZoneId(zone.getId()).stream()
                .map(Batch::getCurrentQuantityKg)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return zone.getCapacityKg() != null ? zone.getCapacityKg().subtract(used) : BigDecimal.ZERO;
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
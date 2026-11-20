package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.CropStorageBatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * API-005: receives the Product Transformation storage entry (dried mango
 * ready after packaging) and stores it as a new batch plus its traceability
 * IN movement, in one transaction: if any write fails, nothing is kept.
 * An unknown zone answers 404, a capacity overflow or an already registered
 * batch_id answers 409 (a concurrent duplicate is caught by the unique batch
 * code and also answers 409, see CropStorageEntryExceptionHandler).
 *
 * <p>The zone row is locked before its capacity is checked, so two concurrent
 * entries (or movements) into the same zone cannot both pass the check.
 */
@Service
@RequiredArgsConstructor
public class StorageEntryService {

    static final String ENTRY_STATUS = "STORED";

    private final CropStorageBatchRepository batchRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional
    public StorageEntryResponse registerEntry(StorageEntryRequest request) {
        if (batchRepository.existsByBatchCode(request.batchId())) {
            throw new ConflictException("Storage entry already registered for batch_id " + request.batchId());
        }
        BigDecimal quantity = request.driedQuantityKg();
        StorageZone zone = lockZoneWithRoom(request, quantity);
        BigDecimal remainingBefore = remainingCapacity(zone);

        CropStorageBatch batch = new CropStorageBatch();
        batch.setBatchCode(request.batchId());
        batch.setProductType("DRIED_MANGO");
        batch.setProductStage("PACKAGED");
        batch.setQualityGrade(request.productGrade());
        batch.setStatus(ENTRY_STATUS);
        batch.setStorageZone(zone);
        batch.setInitialQuantityKg(quantity);
        // The calendar date of packaging in UTC, not in the sender's offset.
        batch.setPackagingDate(request.packagingDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate());
        batch.setStorageEntryDate(LocalDate.now());
        // Validated upstream as digits only: never silently dropped.
        batch.setHarvestId(request.harvestId() == null ? null : Long.valueOf(request.harvestId()));
        CropStorageBatch savedBatch = batchRepository.save(batch);

        StockMovement movement = new StockMovement();
        movement.setType("IN");
        movement.setQuantityKg(quantity);
        movement.setBatch(savedBatch);
        movement.setToZone(zone);
        movement.setReason("Product Transformation storage entry, packaging " + request.packagingId());
        movement.setMovedAt(savedBatch.getCreatedAt() != null ? savedBatch.getCreatedAt() : LocalDateTime.now());
        stockMovementRepository.save(movement);

        return new StorageEntryResponse(
                String.valueOf(savedBatch.getId()),
                String.valueOf(zone.getId()),
                toUtc(savedBatch.getCreatedAt()),
                remainingBefore.subtract(quantity),
                savedBatch.getStatus());
    }

    /**
     * API-005 read of one entry. The storage_entry_id is the Crop Storage batch
     * id returned on creation; status and available capacity are the current ones.
     */
    @Transactional(readOnly = true)
    public StorageEntryResponse findEntry(Long storageEntryId) {
        CropStorageBatch batch = batchRepository.findWithRelationshipsById(storageEntryId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Storage entry not found with id " + storageEntryId));
        return toResponse(batch);
    }

    /**
     * API-005 read of the entries of one Product Transformation batch_id: zero
     * or one element, since a batch_id can be registered only once.
     */
    @Transactional(readOnly = true)
    public List<StorageEntryResponse> findEntriesByBatchId(String batchId) {
        return batchRepository.findByBatchCode(batchId).stream().map(this::toResponse).toList();
    }

    private StorageEntryResponse toResponse(CropStorageBatch batch) {
        StorageZone zone = batch.getStorageZone();
        return new StorageEntryResponse(
                String.valueOf(batch.getId()),
                zone == null ? null : String.valueOf(zone.getId()),
                toUtc(batch.getCreatedAt()),
                zone == null ? null : remainingCapacity(zone),
                batch.getStatus());
    }

    /**
     * Uses the requested zone when given, otherwise the zone with the most room
     * left (HYPOTHESIS until the assignment rule is agreed with Product
     * Transformation). The chosen zone is locked, then its room is checked again.
     */
    private StorageZone lockZoneWithRoom(StorageEntryRequest request, BigDecimal quantity) {
        Long zoneId;
        String requested = request.targetStorageLocationId();
        if (requested != null && !requested.isBlank()) {
            zoneId = parseZoneId(requested);
        } else {
            zoneId = storageZoneRepository.findAll().stream()
                    .filter(z -> remainingCapacity(z).compareTo(quantity) >= 0)
                    .max(Comparator.comparing(this::remainingCapacity))
                    .map(StorageZone::getId)
                    .orElseThrow(() -> new ConflictException("Zone capacity exceeded"));
        }
        StorageZone zone = storageZoneRepository.findForUpdateById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Storage location not found with id " + requested));
        if (remainingCapacity(zone).compareTo(quantity) < 0) {
            throw new ConflictException("Zone capacity exceeded");
        }
        return zone;
    }

    private static Long parseZoneId(String value) {
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            // No verified mapping from a zone code to a Crop Storage id: refuse, do not guess.
            throw new ResourceNotFoundException("Storage location not found with id " + value);
        }
    }

    /** Room left in a zone: capacity minus the stock of the batches inside. */
    private BigDecimal remainingCapacity(StorageZone zone) {
        BigDecimal used = Objects.requireNonNullElse(
                batchRepository.sumCurrentQuantityKgByStorageZoneId(zone.getId()), BigDecimal.ZERO);
        return zone.getCapacityKg() != null ? zone.getCapacityKg().subtract(used) : BigDecimal.ZERO;
    }

    /** Entity timestamps are written with LocalDateTime.now(), i.e. in the JVM zone. */
    private static OffsetDateTime toUtc(LocalDateTime serverLocal) {
        return serverLocal == null
                ? null
                : serverLocal.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime();
    }
}

package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryRequest;
import com.infineonbit.sustainablefarm.modules.cropstorage.dto.StorageEntryResponse;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.BatchStatus;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropProductType;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.MovementType;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.ProductStage;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.CropStorageBatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StorageZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * API-005: receives the Product Transformation storage entry (dried mango
 * ready after packaging) and stores it as a new batch plus its traceability
 * IN movement, in one transaction: if any write fails, nothing is kept.
 * An unknown zone answers 404, a capacity overflow or an already registered
 * batch_id answers 409. A concurrent duplicate passes the existence check but
 * is refused by the unique batch code: it is turned into the same
 * ConflictException here, so the common core handler answers 409 (no module
 * exception handler, ARCHITECTURE.md "errors are handled only in core").
 *
 * <p>The zone row is locked before its capacity is checked, so two concurrent
 * entries (or movements) into the same zone cannot both pass the check.
 */
@Service
@RequiredArgsConstructor
public class StorageEntryService {

    static final BatchStatus ENTRY_STATUS = BatchStatus.STORED;

    private final CropStorageBatchRepository batchRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final StockMovementRepository stockMovementRepository;
    private final SharedReferences sharedReferences;

    @Transactional
    public StorageEntryResponse registerEntry(StorageEntryRequest request) {
        if (batchRepository.existsByBatchCode(request.batchId())) {
            throw new ConflictException("Storage entry already registered for batch_id " + request.batchId());
        }
        // Validated upstream as digits only; it must also be a known Plants harvest (400).
        Long harvestId = request.harvestId() == null ? null : Long.valueOf(request.harvestId());
        sharedReferences.requireHarvest(harvestId);
        BigDecimal quantity = request.driedQuantityKg();
        StorageZone zone = lockZoneWithRoom(request, quantity);
        BigDecimal remainingBefore = remainingCapacity(zone);

        CropStorageBatch batch = new CropStorageBatch();
        batch.setBatchCode(request.batchId());
        batch.setProductType(CropProductType.DRIED_MANGO);
        batch.setProductStage(ProductStage.PACKAGED);
        batch.setQualityGrade(request.productGrade());
        batch.setStatus(ENTRY_STATUS);
        batch.setStorageZone(zone);
        batch.setInitialQuantityKg(quantity);
        // The calendar date of packaging in UTC, not in the sender's offset.
        batch.setPackagingDate(request.packagingDate().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate());
        batch.setStorageEntryDate(LocalDate.now());
        batch.setHarvestId(harvestId);
        CropStorageBatch savedBatch;
        try {
            // Flushed now so that a unique-key refusal surfaces here, inside the transaction.
            savedBatch = batchRepository.saveAndFlush(batch);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Storage entry already registered for batch_id " + request.batchId());
        }

        StockMovement movement = new StockMovement();
        movement.setType(MovementType.IN);
        movement.setQuantityKg(quantity);
        movement.setBatch(savedBatch);
        movement.setToZone(zone);
        movement.setReason("Product Transformation storage entry, packaging " + request.packagingId());
        movement.setMovedAt(savedBatch.getCreatedAt() != null
                ? LocalDateTime.ofInstant(savedBatch.getCreatedAt(), ZoneId.systemDefault())
                : LocalDateTime.now());
        stockMovementRepository.save(movement);

        return new StorageEntryResponse(
                String.valueOf(savedBatch.getId()),
                String.valueOf(zone.getId()),
                toUtc(savedBatch.getCreatedAt()),
                remainingBefore.subtract(quantity),
                savedBatch.getStatus().name());
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
                batch.getStatus() == null ? null : batch.getStatus().name());
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
        StorageZone zone;
        try {
            zone = storageZoneRepository.findForUpdateById(zoneId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Storage location not found with id " + requested));
        } catch (PessimisticLockingFailureException e) {
            // Lock wait timed out: another write holds the zone. Retryable, not a server error.
            throw new ConflictException("Storage zone busy, retry the storage entry");
        }
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

    /**
     * The creation instant in UTC, at the microsecond precision the database keeps, so the
     * creation answer and a later read carry the same entry_timestamp.
     */
    private static OffsetDateTime toUtc(Instant createdAt) {
        return createdAt == null ? null : createdAt.truncatedTo(ChronoUnit.MICROS).atOffset(ZoneOffset.UTC);
    }
}

package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
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
import java.util.List;

/**
 * Applies stock movements (IN, OUT, TRANSFER) to batches, enforcing the
 * stock floor on OUT and the zone capacity on IN and TRANSFER. Business
 * failures are thrown as mapped domain exceptions so the API answers 404,
 * 409 or 422 instead of a generic 500.
 */
@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final CropStorageBatchRepository batchRepository;
    private final StorageZoneRepository storageZoneRepository;

    /**
     * Records one movement and updates the batch stock accordingly.
     *
     * @param movement movement with batch id, type IN/OUT/TRANSFER, quantity
     *                 and optional destination zone id
     * @return the saved movement
     * @throws ResourceNotFoundException when the batch or zone id is unknown
     * @throws BusinessRuleException when the batch is missing, the type is
     *         unknown, an IN or TRANSFER has no destination, or OUT exceeds stock
     * @throws ConflictException when an IN or TRANSFER would overflow the zone
     */
    @Transactional
    public StockMovement create(StockMovement movement) {
        if (movement.getBatch() == null || movement.getBatch().getId() == null) {
            throw new BusinessRuleException("A batch is required");
        }
        // The destination zone is locked BEFORE the batch is read: once the lock is
        // held, every quantity read below is the committed state, and it stays
        // consistent with the check and the write until this transaction ends.
        StorageZone toZone = null;
        if (movement.getToZone() != null) {
            if (movement.getToZone().getId() == null) {
                throw new BusinessRuleException("A destination zone reference requires an id");
            }
            // Row lock: serializes concurrent writes filling the same zone (see findForUpdateById).
            toZone = storageZoneRepository.findForUpdateById(movement.getToZone().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Zone not found with id " + movement.getToZone().getId()));
            movement.setToZone(toZone);
        }

        CropStorageBatch batch = batchRepository.findWithRelationshipsById(movement.getBatch().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Batch not found with id " + movement.getBatch().getId()));

        // The request body carries a bare-id batch shell; since CropStorageBatch has
        // @Version, Hibernate reads a null version as unsaved and refuses
        // to link the movement. Attach the managed batch instead.
        movement.setBatch(batch);
        if (movement.getFromZone() != null) {
            if (movement.getFromZone().getId() == null) {
                throw new BusinessRuleException("A source zone reference requires an id");
            }
            StorageZone fromZone = storageZoneRepository.findById(movement.getFromZone().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Zone not found with id " + movement.getFromZone().getId()));
            movement.setFromZone(fromZone);
        }

        BigDecimal quantity = movement.getQuantityKg();
        BigDecimal current = batch.getCurrentQuantityKg() != null
                ? batch.getCurrentQuantityKg() : BigDecimal.ZERO;
        String type = movement.getType();

        if ("IN".equals(type)) {
            // Server-side twin of the movement form rule: stock always enters a zone,
            // otherwise it would escape every capacity check.
            if (toZone == null) {
                throw new BusinessRuleException("A destination zone is required for IN");
            }
            BigDecimal resulting = current.add(quantity);
            // toZone is row-locked above; the check reads fresh sums and the write
            // below commits in the same transaction, so no concurrent fill can interleave.
            checkInCapacity(toZone, batch, resulting);
            batch.setStorageZone(toZone);
            batch.setCurrentQuantityKg(resulting);
        } else if ("OUT".equals(type)) {
            if (current.compareTo(quantity) < 0) {
                throw new BusinessRuleException("Not enough stock in batch");
            }
            batch.setCurrentQuantityKg(current.subtract(quantity));
        } else if ("TRANSFER".equals(type)) {
            if (toZone == null) {
                throw new BusinessRuleException("A destination zone is required for TRANSFER");
            }
            checkCapacity(toZone, batch, current);
            batch.setStorageZone(toZone);
        } else {
            throw new BusinessRuleException("Unknown movement type: " + type);
        }

        batchRepository.save(batch);
        return stockMovementRepository.save(movement);
    }

    /**
     * @return every movement, all batches mixed
     */
    public List<StockMovement> findAll() {
        return stockMovementRepository.findAll();
    }

    /**
     * @param batchId batch to filter on
     * @return movements of that batch, most recent first
     */
    public List<StockMovement> findByBatch(Long batchId) {
        return stockMovementRepository.findByBatchIdOrderByMovedAtDesc(batchId);
    }

    /**
     * IN rule: the other batches of the locked zone plus the batch after the
     * entry must fit in capacityKg. Uses a scalar query, so the sum is read from
     * the database after the lock, never from stale managed entities.
     */
    private void checkInCapacity(StorageZone zone, CropStorageBatch movedBatch, BigDecimal resultingKg) {
        if (zone.getCapacityKg() == null) {
            return;
        }
        BigDecimal others = java.util.Objects.requireNonNullElse(
                batchRepository.sumCurrentQuantityKgByStorageZoneIdExcludingBatch(zone.getId(), movedBatch.getId()),
                BigDecimal.ZERO);
        if (others.add(resultingKg).compareTo(zone.getCapacityKg()) > 0) {
            throw new ConflictException("Zone capacity exceeded");
        }
    }

    // TRANSFER rule (unchanged). HYPOTHESIS: total of batch quantities in a zone must stay under capacityKg
    private void checkCapacity(StorageZone zone, CropStorageBatch movedBatch, BigDecimal resultingKg) {
        if (zone.getCapacityKg() == null) {
            return;
        }
        BigDecimal others = batchRepository.findByStorageZoneId(zone.getId()).stream()
                .filter(b -> !b.getId().equals(movedBatch.getId()))
                .map(CropStorageBatch::getCurrentQuantityKg)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (others.add(resultingKg).compareTo(zone.getCapacityKg()) > 0) {
            throw new ConflictException("Zone capacity exceeded");
        }
    }
}
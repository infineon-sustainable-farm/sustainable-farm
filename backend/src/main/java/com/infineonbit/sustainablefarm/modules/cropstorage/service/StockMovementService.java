package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;
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
    private final BatchRepository batchRepository;
    private final StorageZoneRepository storageZoneRepository;

    /**
     * Records one movement and updates the batch stock accordingly.
     *
     * @param movement movement with batch id, type IN/OUT/TRANSFER, quantity
     *                 and optional destination zone id
     * @return the saved movement
     * @throws ResourceNotFoundException when the batch or zone id is unknown
     * @throws BusinessRuleException when the batch is missing, the type is
     *         unknown, a TRANSFER has no destination, or OUT exceeds stock
     * @throws ConflictException when an IN or TRANSFER would overflow the zone
     */
    @Transactional
    public StockMovement create(StockMovement movement) {
        if (movement.getBatch() == null || movement.getBatch().getId() == null) {
            throw new BusinessRuleException("A batch is required");
        }
        Batch batch = batchRepository.findById(movement.getBatch().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Batch not found with id " + movement.getBatch().getId()));

        StorageZone toZone = null;
        if (movement.getToZone() != null && movement.getToZone().getId() != null) {
            toZone = storageZoneRepository.findById(movement.getToZone().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Zone not found with id " + movement.getToZone().getId()));
        }

        BigDecimal quantity = movement.getQuantityKg();
        BigDecimal current = batch.getCurrentQuantityKg() != null
                ? batch.getCurrentQuantityKg() : BigDecimal.ZERO;
        String type = movement.getType();

        if ("IN".equals(type)) {
            BigDecimal resulting = current.add(quantity);
            if (toZone != null) {
                checkCapacity(toZone, batch, resulting);
                batch.setStorageZone(toZone);
            }
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

    // HYPOTHESIS: total of batch quantities in a zone must stay under capacityKg
    private void checkCapacity(StorageZone zone, Batch movedBatch, BigDecimal resultingKg) {
        if (zone.getCapacityKg() == null) {
            return;
        }
        BigDecimal others = batchRepository.findByStorageZoneId(zone.getId()).stream()
                .filter(b -> !b.getId().equals(movedBatch.getId()))
                .map(Batch::getCurrentQuantityKg)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (others.add(resultingKg).compareTo(zone.getCapacityKg()) > 0) {
            throw new ConflictException("Zone capacity exceeded");
        }
    }
}
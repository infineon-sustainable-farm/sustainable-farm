package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StorageZone;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.BatchRepository;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final BatchRepository batchRepository;

    @Transactional
    public StockMovement create(StockMovement movement) {
        if (movement.getBatch() == null || movement.getBatch().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A batch is required");
        }
        Batch batch = batchRepository.findById(movement.getBatch().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch not found"));

        BigDecimal quantity = movement.getQuantityKg();
        BigDecimal current = batch.getCurrentQuantityKg() != null
                ? batch.getCurrentQuantityKg() : BigDecimal.ZERO;
        String type = movement.getType();

        if ("IN".equals(type)) {
            BigDecimal resulting = current.add(quantity);
            if (movement.getToZone() != null) {
                checkCapacity(movement.getToZone(), batch, resulting);
                batch.setStorageZone(movement.getToZone());
            }
            batch.setCurrentQuantityKg(resulting);
        } else if ("OUT".equals(type)) {
            if (current.compareTo(quantity) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stock in batch");
            }
            batch.setCurrentQuantityKg(current.subtract(quantity));
        } else if ("TRANSFER".equals(type)) {
            if (movement.getToZone() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A destination zone is required for TRANSFER");
            }
            checkCapacity(movement.getToZone(), batch, current);
            batch.setStorageZone(movement.getToZone());
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown movement type: " + type);
        }

        batchRepository.save(batch);
        return stockMovementRepository.save(movement);
    }

    public List<StockMovement> findAll() {
        return stockMovementRepository.findAll();
    }

    public List<StockMovement> findByBatch(Long batchId) {
        return stockMovementRepository.findByBatchIdOrderByMovedAtDesc(batchId);
    }

    // HYPOTHESIS: total of batch quantities in a zone must stay under capacityKg
    private void checkCapacity(StorageZone zone, Batch movedBatch, BigDecimal resultingKg) {
        if (zone.getId() == null || zone.getCapacityKg() == null) {
            return;
        }
        BigDecimal others = batchRepository.findByStorageZoneId(zone.getId()).stream()
                .filter(b -> !b.getId().equals(movedBatch.getId()))
                .map(Batch::getCurrentQuantityKg)
                .filter(q -> q != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (others.add(resultingKg).compareTo(zone.getCapacityKg()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Zone capacity exceeded");
        }
    }
}
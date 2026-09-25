package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import com.infineonbit.sustainablefarm.modules.cropstorage.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;

    public StockMovement create(StockMovement movement) {
        return stockMovementRepository.save(movement);
    }

    public List<StockMovement> findAll() {
        return stockMovementRepository.findAll();
    }

    public List<StockMovement> findByBatch(Long batchId) {
        return stockMovementRepository.findByBatchIdOrderByMovedAtDesc(batchId);
    }
}
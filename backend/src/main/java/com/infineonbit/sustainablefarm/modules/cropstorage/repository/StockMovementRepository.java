package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.StockMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "batch", "batch.supplier", "batch.storageZone", "fromZone", "toZone"
    })
    List<StockMovement> findAll();

    @EntityGraph(attributePaths = {
            "batch", "batch.supplier", "batch.storageZone", "fromZone", "toZone"
    })
    List<StockMovement> findByBatchIdOrderByMovedAtDesc(Long batchId);
}
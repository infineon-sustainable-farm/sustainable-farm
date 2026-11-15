package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.CropStorageBatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository("cropStorageBatchRepository")
public interface CropStorageBatchRepository extends JpaRepository<CropStorageBatch, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<CropStorageBatch> findAll();

    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    @Query("select b from CropStorageBatch b where b.id = :id")
    Optional<CropStorageBatch> findWithRelationshipsById(@Param("id") Long id);

    // Anar: FIFO with priority to important customers first
    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<CropStorageBatch> findByCurrentQuantityKgGreaterThanOrderByCustomerPriorityDescStorageEntryDateAsc(BigDecimal minQuantity);

    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<CropStorageBatch> findByStorageZoneId(Long storageZoneId);

    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<CropStorageBatch> findByExpiryDateNotNullOrderByExpiryDateAsc();

    // API-005 dedup: reject a second storage entry for the same PT batch id
    boolean existsByBatchCode(String batchCode);
}
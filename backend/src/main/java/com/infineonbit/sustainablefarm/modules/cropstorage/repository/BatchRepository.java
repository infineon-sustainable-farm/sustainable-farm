package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    @Override
    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<Batch> findAll();

    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    @Query("select b from Batch b where b.id = :id")
    Optional<Batch> findWithRelationshipsById(@Param("id") Long id);

    // Anar: FIFO with priority to important customers first
    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<Batch> findByCurrentQuantityKgGreaterThanOrderByCustomerPriorityDescStorageEntryDateAsc(BigDecimal minQuantity);

    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<Batch> findByStorageZoneId(Long storageZoneId);

    @EntityGraph(attributePaths = {"supplier", "storageZone"})
    List<Batch> findByExpiryDateNotNullOrderByExpiryDateAsc();

    // API-005 dedup: reject a second storage entry for the same PT batch id
    boolean existsByBatchCode(String batchCode);
}
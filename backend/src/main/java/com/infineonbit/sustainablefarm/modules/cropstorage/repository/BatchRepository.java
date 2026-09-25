package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    // Anar: FIFO with priority to important customers first
    List<Batch> findByCurrentQuantityKgGreaterThanOrderByCustomerPriorityDescStorageEntryDateAsc(BigDecimal minQuantity);

    List<Batch> findByStorageZoneId(Long storageZoneId);

    List<Batch> findByExpiryDateNotNullOrderByExpiryDateAsc();
}
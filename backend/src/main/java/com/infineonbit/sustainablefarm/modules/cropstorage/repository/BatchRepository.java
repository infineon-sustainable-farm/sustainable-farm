package com.infineonbit.sustainablefarm.modules.cropstorage.repository;

import com.infineonbit.sustainablefarm.modules.cropstorage.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByCurrentQuantityKgGreaterThanOrderByStorageEntryDateAsc(BigDecimal minQuantity);

    List<Batch> findByStorageZoneId(Long storageZoneId);
}
package com.infineonbit.sustainablefarm.modules.salesmarketing.repository;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBatchRepository extends JpaRepository<ProductBatch, Integer> {
    // Basic CRUD comes from JpaRepository.
}

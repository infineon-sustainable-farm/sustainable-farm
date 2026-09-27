package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.modules.salesmarketing.common.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductBatchService {

    private final ProductBatchRepository repository;

    public ProductBatchService(ProductBatchRepository repository) {
        this.repository = repository;
    }

    public List<ProductBatch> getAll() {
        return repository.findAll();
    }

    public ProductBatch getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductBatch not found with id: " + id));
    }

    public ProductBatch create(ProductBatch item) {
        return repository.save(item);
    }

    public ProductBatch update(Integer id, ProductBatch updatedData) {
        ProductBatch existing = getById(id);

        existing.setBatchCode(updatedData.getBatchCode());
        existing.setProductId(updatedData.getProductId());
        existing.setStockT(updatedData.getStockT());
        existing.setStatus(updatedData.getStatus());
        existing.setSyncedAt(updatedData.getSyncedAt());

        return repository.save(existing);
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}

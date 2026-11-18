package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLot;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLotLookup;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductBatchService {

    private final ProductBatchRepository repository;
    private final PackagedLotLookup packagedLots;

    public ProductBatchService(ProductBatchRepository repository, PackagedLotLookup packagedLots) {
        this.repository = repository;
        this.packagedLots = packagedLots;
    }

    public List<ProductBatch> getAll() {
        return repository.findAll();
    }

    public ProductBatch getById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductBatch not found with id: " + id));
    }

    public ProductBatch create(ProductBatch item) {
        linkPackagedLot(item);
        return repository.save(item);
    }

    public ProductBatch update(Integer id, ProductBatch updatedData) {
        ProductBatch existing = getById(id);

        existing.setBatchCode(updatedData.getBatchCode());
        existing.setProductId(updatedData.getProductId());
        existing.setStockT(updatedData.getStockT());
        existing.setStatus(updatedData.getStatus());
        existing.setSyncedAt(updatedData.getSyncedAt());
        linkPackagedLot(existing);

        return repository.save(existing);
    }

    /**
     * Links the batch to the product transformation packaged lot whose lot code is its batch
     * code, or clears the link when there is none. Only the link is written: the stock and the
     * status stay those of the request.
     */
    private void linkPackagedLot(ProductBatch batch) {
        batch.setPackagingRecordId(packagedLots.findByLotCode(batch.getBatchCode())
                .map(PackagedLot::recordId)
                .orElse(null));
    }

    public void delete(Integer id) {
        repository.deleteById(id);
    }
}

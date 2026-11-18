package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.ResourceNotFoundException;

import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLot;
import com.infineonbit.sustainablefarm.modules.producttransformation.api.PackagedLotLookup;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductBatchRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ProductBatchResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import com.infineonbit.sustainablefarm.modules.salesmarketing.repository.ProductBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductBatchService {

    private final ProductBatchRepository repository;
    private final PackagedLotLookup packagedLots;
    private final SalesMarketingRequestMapper mapper;

    public ProductBatchService(ProductBatchRepository repository, PackagedLotLookup packagedLots,
                               SalesMarketingRequestMapper mapper) {
        this.repository = repository;
        this.packagedLots = packagedLots;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<ProductBatchResponse> getAll() {
        return repository.findAll().stream().map(ProductBatchResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public ProductBatchResponse getById(Long id) {
        return ProductBatchResponse.of(find(id));
    }

    public ProductBatchResponse create(ProductBatchRequest request) {
        ProductBatch item = mapper.apply(request, new ProductBatch());
        linkPackagedLot(item);
        return ProductBatchResponse.of(repository.save(item));
    }

    public ProductBatchResponse update(Long id, ProductBatchRequest request) {
        ProductBatch existing = mapper.apply(request, find(id));
        linkPackagedLot(existing);

        return ProductBatchResponse.of(repository.save(existing));
    }

    private ProductBatch find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductBatch not found with id: " + id));
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

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

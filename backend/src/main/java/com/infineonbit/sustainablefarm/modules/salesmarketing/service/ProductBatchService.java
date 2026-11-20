package com.infineonbit.sustainablefarm.modules.salesmarketing.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
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
        PackagedLot lot = packagedLot(request.batchCode());
        ProductBatch item = mapper.apply(request, new ProductBatch());
        link(item, lot);
        return ProductBatchResponse.of(repository.save(item));
    }

    public ProductBatchResponse update(Long id, ProductBatchRequest request) {
        ProductBatch existing = find(id);
        PackagedLot lot = packagedLot(request.batchCode());
        link(mapper.apply(request, existing), lot);
        return ProductBatchResponse.of(repository.save(existing));
    }

    private ProductBatch find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductBatch not found with id: " + id));
    }

    /**
     * The product transformation packaged lot a batch code designates. Product transformation
     * owns the packaged lots: sales picks one by its lot code
     * ({@code GET /api/product-transformation/packaging-records}) and never types a new one.
     *
     * @throws BadRequestException (400) when no packaged lot has this code
     */
    private PackagedLot packagedLot(String batchCode) {
        return packagedLots.findByLotCode(batchCode)
                .orElseThrow(() -> new BadRequestException(
                        "Unknown packaged lot, not a lot code of product transformation: " + batchCode));
    }

    /**
     * Links the batch to its packaged lot, under the lot code. Only the link is written: the stock
     * and the status stay those of the request.
     */
    private static void link(ProductBatch batch, PackagedLot lot) {
        batch.setBatchCode(lot.lotCode());
        batch.setPackagingRecordId(lot.id());
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

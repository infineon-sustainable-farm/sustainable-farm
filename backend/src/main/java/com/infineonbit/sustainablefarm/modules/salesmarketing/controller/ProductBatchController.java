package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ProductBatchService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductBatchRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-batches")
public class ProductBatchController {

    private final ProductBatchService service;
    private final SalesMarketingRequestMapper requestMapper;

    public ProductBatchController(ProductBatchService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<ProductBatch> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProductBatch getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBatch create(@Valid @RequestBody ProductBatchRequest request) {
        return service.create(requestMapper.toEntity(request, ProductBatch.class));
    }

    @PutMapping("/{id}")
    public ProductBatch update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductBatchRequest request) {
        return service.update(id, requestMapper.toEntity(request, ProductBatch.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

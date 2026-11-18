package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductBatchRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.ProductBatchResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ProductBatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/product-batches")
public class ProductBatchController {

    private final ProductBatchService service;

    public ProductBatchController(ProductBatchService service) {
        this.service = service;
    }

    @GetMapping
    public List<ProductBatchResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ProductBatchResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductBatchResponse create(@Valid @RequestBody ProductBatchRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ProductBatchResponse update(@PathVariable Long id, @Valid @RequestBody ProductBatchRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.ProductBatch;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ProductBatchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-batches")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductBatchController {

    private final ProductBatchService service;

    public ProductBatchController(ProductBatchService service) {
        this.service = service;
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
    public ProductBatch create(@Valid @RequestBody ProductBatch item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public ProductBatch update(
            @PathVariable Integer id,
            @Valid @RequestBody ProductBatch item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}

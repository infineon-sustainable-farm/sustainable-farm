package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.Product;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.ProductService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.ProductRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;
    private final SalesMarketingRequestMapper requestMapper;

    public ProductController(ProductService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<Product> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Product getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return service.create(requestMapper.toEntity(request, Product.class));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Integer id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, requestMapper.toEntity(request, Product.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
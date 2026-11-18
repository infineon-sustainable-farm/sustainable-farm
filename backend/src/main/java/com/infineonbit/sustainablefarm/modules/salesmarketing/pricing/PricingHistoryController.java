package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingHistoryRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.PricingHistoryResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.PricingHistoryService;
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
@RequestMapping("/api/pricing-history")
public class PricingHistoryController {

    private final PricingHistoryService service;

    public PricingHistoryController(PricingHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<PricingHistoryResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PricingHistoryResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PricingHistoryResponse create(@Valid @RequestBody PricingHistoryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PricingHistoryResponse update(@PathVariable Long id, @Valid @RequestBody PricingHistoryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

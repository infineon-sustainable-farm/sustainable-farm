package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.PricingHistoryService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingHistoryRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-history")
public class PricingHistoryController {

    private final PricingHistoryService service;
    private final SalesMarketingRequestMapper requestMapper;

    public PricingHistoryController(PricingHistoryService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<PricingHistory> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PricingHistory getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PricingHistory create(@Valid @RequestBody PricingHistoryRequest request) {
        return service.create(requestMapper.toEntity(request, PricingHistory.class));
    }

    @PutMapping("/{id}")
    public PricingHistory update(
            @PathVariable Integer id,
            @Valid @RequestBody PricingHistoryRequest request) {
        return service.update(id, requestMapper.toEntity(request, PricingHistory.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
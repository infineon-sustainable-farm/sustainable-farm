package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CompetitorPrice;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CompetitorPriceService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CompetitorPriceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/competitor-prices")
public class CompetitorPriceController {

    private final CompetitorPriceService service;
    private final SalesMarketingRequestMapper requestMapper;

    public CompetitorPriceController(CompetitorPriceService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<CompetitorPrice> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CompetitorPrice getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompetitorPrice create(@Valid @RequestBody CompetitorPriceRequest request) {
        return service.create(requestMapper.toEntity(request, CompetitorPrice.class));
    }

    @PutMapping("/{id}")
    public CompetitorPrice update(
            @PathVariable Integer id,
            @Valid @RequestBody CompetitorPriceRequest request) {
        return service.update(id, requestMapper.toEntity(request, CompetitorPrice.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingHistory;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.PricingHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-history")
@CrossOrigin(origins = "http://localhost:3000")
public class PricingHistoryController {

    private final PricingHistoryService service;

    public PricingHistoryController(PricingHistoryService service) {
        this.service = service;
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
    public PricingHistory create(@Valid @RequestBody PricingHistory item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public PricingHistory update(
            @PathVariable Integer id,
            @Valid @RequestBody PricingHistory item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
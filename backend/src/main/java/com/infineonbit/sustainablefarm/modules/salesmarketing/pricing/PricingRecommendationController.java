package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingRecommendation;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.PricingRecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-recommendations")
@CrossOrigin(origins = "http://localhost:3000")
public class PricingRecommendationController {

    private final PricingRecommendationService service;

    public PricingRecommendationController(PricingRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    public List<PricingRecommendation> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PricingRecommendation getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PricingRecommendation create(
            @Valid @RequestBody PricingRecommendation item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public PricingRecommendation update(
            @PathVariable Integer id,
            @Valid @RequestBody PricingRecommendation item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
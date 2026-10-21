package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.PricingRecommendation;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.PricingRecommendationService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingRecommendationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pricing-recommendations")
public class PricingRecommendationController {

    private final PricingRecommendationService service;
    private final SalesMarketingRequestMapper requestMapper;

    public PricingRecommendationController(
            PricingRecommendationService service,
            SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
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
            @Valid @RequestBody PricingRecommendationRequest request) {
        return service.create(requestMapper.toEntity(request, PricingRecommendation.class));
    }

    @PutMapping("/{id}")
    public PricingRecommendation update(
            @PathVariable Integer id,
            @Valid @RequestBody PricingRecommendationRequest request) {
        return service.update(id, requestMapper.toEntity(request, PricingRecommendation.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
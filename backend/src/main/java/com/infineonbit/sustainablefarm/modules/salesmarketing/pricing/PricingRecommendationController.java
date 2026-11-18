package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.PricingRecommendationRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.PricingRecommendationResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.PricingRecommendationService;
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
@RequestMapping("/api/pricing-recommendations")
public class PricingRecommendationController {

    private final PricingRecommendationService service;

    public PricingRecommendationController(PricingRecommendationService service) {
        this.service = service;
    }

    @GetMapping
    public List<PricingRecommendationResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public PricingRecommendationResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PricingRecommendationResponse create(@Valid @RequestBody PricingRecommendationRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PricingRecommendationResponse update(@PathVariable Long id, @Valid @RequestBody PricingRecommendationRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

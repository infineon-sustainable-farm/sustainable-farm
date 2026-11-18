package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignMetricRequest;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingResponses.CampaignMetricResponse;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CampaignMetricService;
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
@RequestMapping("/api/campaign-metrics")
public class CampaignMetricController {

    private final CampaignMetricService service;

    public CampaignMetricController(CampaignMetricService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampaignMetricResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CampaignMetricResponse getOne(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignMetricResponse create(@Valid @RequestBody CampaignMetricRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CampaignMetricResponse update(@PathVariable Long id, @Valid @RequestBody CampaignMetricRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

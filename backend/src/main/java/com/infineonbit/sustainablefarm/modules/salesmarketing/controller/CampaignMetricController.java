package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignMetric;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CampaignMetricService;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequestMapper;
import com.infineonbit.sustainablefarm.modules.salesmarketing.dto.SalesMarketingRequests.CampaignMetricRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaign-metrics")
public class CampaignMetricController {

    private final CampaignMetricService service;
    private final SalesMarketingRequestMapper requestMapper;

    public CampaignMetricController(CampaignMetricService service, SalesMarketingRequestMapper requestMapper) {
        this.service = service;
        this.requestMapper = requestMapper;
    }

    @GetMapping
    public List<CampaignMetric> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CampaignMetric getOne(@PathVariable Integer id) {
        return service.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignMetric create(@Valid @RequestBody CampaignMetricRequest request) {
        return service.create(requestMapper.toEntity(request, CampaignMetric.class));
    }

    @PutMapping("/{id}")
    public CampaignMetric update(
            @PathVariable Integer id,
            @Valid @RequestBody CampaignMetricRequest request) {
        return service.update(id, requestMapper.toEntity(request, CampaignMetric.class));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
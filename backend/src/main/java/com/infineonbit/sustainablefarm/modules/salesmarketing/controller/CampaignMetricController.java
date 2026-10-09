package com.infineonbit.sustainablefarm.modules.salesmarketing.controller;

import com.infineonbit.sustainablefarm.modules.salesmarketing.entity.CampaignMetric;
import com.infineonbit.sustainablefarm.modules.salesmarketing.service.CampaignMetricService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/campaign-metrics")
@CrossOrigin(origins = "http://localhost:3000")
public class CampaignMetricController {

    private final CampaignMetricService service;

    public CampaignMetricController(CampaignMetricService service) {
        this.service = service;
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
    public CampaignMetric create(
            @Valid @RequestBody CampaignMetric item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    public CampaignMetric update(
            @PathVariable Integer id,
            @Valid @RequestBody CampaignMetric item) {
        return service.update(id, item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id) {
        service.delete(id);
    }
}
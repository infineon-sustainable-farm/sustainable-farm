package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.CarbonMetric;
import com.infineonbit.sustainablefarm.modules.energysupply.service.CarbonMetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Feeds the "Carbon Footprint" tab of the Energy App. */
@RestController
@RequestMapping("/api/energy/carbon")
public class CarbonController {

    private final CarbonMetricService service;

    @Autowired
    public CarbonController(CarbonMetricService service) {
        this.service = service;
    }

    @GetMapping
    public List<CarbonMetric> getAll() {
        return service.findAll();
    }

    @PostMapping
    public CarbonMetric create(@RequestBody CarbonMetric metric) {
        return service.save(metric);
    }
}

package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.SolarGenerationLog;
import com.infineonbit.sustainablefarm.modules.energysupply.service.SolarGenerationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Feeds the "Solar & Generation Monitoring" tab of the Energy App. */
@RestController
@RequestMapping("/api/energy/solar")
public class SolarGenerationController {

    private final SolarGenerationLogService service;

    @Autowired
    public SolarGenerationController(SolarGenerationLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolarGenerationLog> getAll() {
        return service.findAll();
    }

    @GetMapping("/last-7-days")
    public List<SolarGenerationLog> getLast7Days() {
        return service.findLast7Days();
    }

    @PostMapping
    public SolarGenerationLog create(@RequestBody SolarGenerationLog log) {
        return service.save(log);
    }
}

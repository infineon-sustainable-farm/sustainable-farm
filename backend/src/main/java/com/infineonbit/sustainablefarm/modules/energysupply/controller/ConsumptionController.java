package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.ConsumptionLog;
import com.infineonbit.sustainablefarm.modules.energysupply.entity.LoadCategory;
import com.infineonbit.sustainablefarm.modules.energysupply.service.ConsumptionLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Feeds the "Energy Consumption Tracking" tab of the Energy App. */
@RestController
@RequestMapping("/api/energy/consumption")
public class ConsumptionController {

    private final ConsumptionLogService service;

    @Autowired
    public ConsumptionController(ConsumptionLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ConsumptionLog> getAll() {
        return service.findAll();
    }

    @GetMapping("/today")
    public List<ConsumptionLog> getToday() {
        return service.findToday();
    }

    @GetMapping("/breakdown/{category}")
    public List<ConsumptionLog> getByCategory(@PathVariable LoadCategory category) {
        return service.findByCategory(category);
    }

    @PostMapping
    public ConsumptionLog create(@RequestBody ConsumptionLog log) {
        return service.save(log);
    }
}

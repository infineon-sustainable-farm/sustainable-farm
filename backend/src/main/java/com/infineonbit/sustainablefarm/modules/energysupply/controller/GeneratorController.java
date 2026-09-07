package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.entity.GeneratorEvent;
import com.infineonbit.sustainablefarm.modules.energysupply.service.GeneratorEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Feeds the "Generator Backup Management" tab of the Energy App. */
@RestController
@RequestMapping("/api/energy/generator")
public class GeneratorController {

    private final GeneratorEventService service;

    @Autowired
    public GeneratorController(GeneratorEventService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public List<GeneratorEvent> getAllEvents() {
        return service.findAll();
    }

    @PostMapping("/events")
    public GeneratorEvent createEvent(@RequestBody GeneratorEvent event) {
        return service.save(event);
    }
}

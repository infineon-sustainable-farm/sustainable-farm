package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.RainwaterHarvestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rainwater-harvests")
public class RainwaterHarvestController {
    private final RainwaterHarvestService rainwaterHarvestService;

    public RainwaterHarvestController(RainwaterHarvestService rainwaterHarvestService) {
        this.rainwaterHarvestService = rainwaterHarvestService;
    }

    @GetMapping
    public List<RainwaterHarvest> list() {
        return rainwaterHarvestService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RainwaterHarvest create(@Valid @RequestBody RainwaterHarvest harvest) {
        return rainwaterHarvestService.create(harvest);
    }

    @GetMapping("/{harvestId}")
    public RainwaterHarvest get(@PathVariable UUID harvestId) {
        return rainwaterHarvestService.get(harvestId);
    }

    @PutMapping("/{harvestId}")
    public RainwaterHarvest update(@PathVariable UUID harvestId, @RequestBody RainwaterHarvest payload) {
        return rainwaterHarvestService.update(harvestId, payload);
    }

    @DeleteMapping("/{harvestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID harvestId) {
        rainwaterHarvestService.delete(harvestId);
    }

    @GetMapping("/coverage")
    public Map<String, Object> coverage(@RequestParam(defaultValue = "month") String period) {
        return rainwaterHarvestService.coverage(period);
    }
}

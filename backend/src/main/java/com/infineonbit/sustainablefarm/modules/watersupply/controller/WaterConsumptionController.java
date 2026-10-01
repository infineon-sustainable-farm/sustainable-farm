package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterConsumptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/api/water/consumption")
public class WaterConsumptionController {
    private final WaterConsumptionService waterConsumptionService;

    public WaterConsumptionController(WaterConsumptionService waterConsumptionService) {
        this.waterConsumptionService = waterConsumptionService;
    }

    @GetMapping
    public Object consumption(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID farmId,
            @RequestParam(required = false) UUID sourceId) {
        if (page == null && size == null && farmId == null && sourceId == null) {
            return waterConsumptionService.findConsumptions();
        }
        return waterConsumptionService.findConsumptions(pageRequest(page, size), farmId, sourceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaterConsumptionResponse createConsumption(@Valid @RequestBody WaterConsumptionCreateRequest request) {
        return waterConsumptionService.createConsumption(request);
    }

    @GetMapping("/{consumptionId}")
    public WaterConsumptionResponse consumption(@PathVariable UUID consumptionId) {
        return waterConsumptionService.getConsumption(consumptionId);
    }

    @PutMapping("/{consumptionId}")
    public WaterConsumptionResponse updateConsumption(
            @PathVariable UUID consumptionId,
            @RequestBody WaterConsumptionUpdateRequest request) {
        return waterConsumptionService.updateConsumption(consumptionId, request);
    }

    @DeleteMapping("/{consumptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConsumption(@PathVariable UUID consumptionId) {
        waterConsumptionService.deleteConsumption(consumptionId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
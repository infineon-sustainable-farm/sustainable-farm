package com.infineonbit.sustainablefarm.modules.farm.controller;

import com.infineonbit.sustainablefarm.modules.farm.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.FarmResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.FarmUpdateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.farm.dto.ZoneUpdateRequest;
import com.infineonbit.sustainablefarm.modules.farm.service.FarmService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api")
public class FarmController {
    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @GetMapping("/farms")
    public Object farms(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size) {
        if (page == null && size == null) {
            return farmService.findFarms();
        }
        return farmService.findFarms(pageRequest(page, size));
    }

    @PostMapping("/farms")
    @ResponseStatus(HttpStatus.CREATED)
    public FarmResponse createFarm(@Valid @RequestBody FarmCreateRequest request) {
        return farmService.createFarm(request);
    }

    @GetMapping("/farms/{farmId}")
    public FarmResponse farm(@PathVariable Long farmId) {
        return farmService.getFarm(farmId);
    }

    @PutMapping("/farms/{farmId}")
    public FarmResponse updateFarm(@PathVariable Long farmId, @RequestBody FarmUpdateRequest request) {
        return farmService.updateFarm(farmId, request);
    }

    @DeleteMapping("/farms/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFarm(@PathVariable Long farmId) {
        farmService.deleteFarm(farmId);
    }

    @GetMapping("/fields/{fieldId}/zones")
    public Object fieldZones(
            @PathVariable Long fieldId,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size) {
        if (page == null && size == null) {
            return farmService.findFieldZones(fieldId);
        }
        return farmService.findFieldZones(fieldId, pageRequest(page, size));
    }

    @GetMapping("/zones")
    public Object zones(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) Long fieldId) {
        if (page == null && size == null && fieldId == null) {
            return farmService.findZones();
        }
        PageRequest pageable = pageRequest(page, size);
        return fieldId == null ? farmService.findZones(pageable) : farmService.findFieldZones(fieldId, pageable);
    }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse createZone(@Valid @RequestBody ZoneCreateRequest request) {
        return farmService.createZone(request);
    }

    @GetMapping("/zones/{zoneId}")
    public ZoneResponse zone(@PathVariable Long zoneId) {
        return farmService.getZone(zoneId);
    }

    @PutMapping("/zones/{zoneId}")
    public ZoneResponse updateZone(@PathVariable Long zoneId, @RequestBody ZoneUpdateRequest request) {
        return farmService.updateZone(zoneId, request);
    }

    @DeleteMapping("/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZone(@PathVariable Long zoneId) {
        farmService.deleteZone(zoneId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}

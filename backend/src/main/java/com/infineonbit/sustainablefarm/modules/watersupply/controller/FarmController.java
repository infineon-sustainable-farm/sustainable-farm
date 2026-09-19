package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FieldUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.FarmService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
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
    public FarmResponse farm(@PathVariable UUID farmId) {
        return farmService.getFarm(farmId);
    }

    @PutMapping("/farms/{farmId}")
    public FarmResponse updateFarm(@PathVariable UUID farmId, @RequestBody FarmUpdateRequest request) {
        return farmService.updateFarm(farmId, request);
    }

    @DeleteMapping("/farms/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFarm(@PathVariable UUID farmId) {
        farmService.deleteFarm(farmId);
    }

    @GetMapping("/farms/{farmId}/fields")
    public Object farmFields(
            @PathVariable UUID farmId,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size) {
        if (page == null && size == null) {
            return farmService.findFarmFields(farmId);
        }
        return farmService.findFarmFields(farmId, pageRequest(page, size));
    }

    @GetMapping("/fields")
    public Object fields(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID farmId) {
        if (page == null && size == null && farmId == null) {
            return farmService.findFields();
        }
        PageRequest pageable = pageRequest(page, size);
        return farmId == null ? farmService.findFields(pageable) : farmService.findFarmFields(farmId, pageable);
    }

    @PostMapping("/fields")
    @ResponseStatus(HttpStatus.CREATED)
    public FieldResponse createField(@Valid @RequestBody FieldCreateRequest request) {
        return farmService.createField(request);
    }

    @GetMapping("/fields/{fieldId}")
    public FieldResponse field(@PathVariable UUID fieldId) {
        return farmService.getField(fieldId);
    }

    @PutMapping("/fields/{fieldId}")
    public FieldResponse updateField(@PathVariable UUID fieldId, @RequestBody FieldUpdateRequest request) {
        return farmService.updateField(fieldId, request);
    }

    @DeleteMapping("/fields/{fieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteField(@PathVariable UUID fieldId) {
        farmService.deleteField(fieldId);
    }

    @GetMapping("/fields/{fieldId}/zones")
    public Object fieldZones(
            @PathVariable UUID fieldId,
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
            @RequestParam(required = false) UUID fieldId) {
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
    public ZoneResponse zone(@PathVariable UUID zoneId) {
        return farmService.getZone(zoneId);
    }

    @PutMapping("/zones/{zoneId}")
    public ZoneResponse updateZone(@PathVariable UUID zoneId, @RequestBody ZoneUpdateRequest request) {
        return farmService.updateZone(zoneId, request);
    }

    @DeleteMapping("/zones/{zoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteZone(@PathVariable UUID zoneId) {
        farmService.deleteZone(zoneId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}

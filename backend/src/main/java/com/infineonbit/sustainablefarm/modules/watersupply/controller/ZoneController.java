package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.ZoneService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ZoneController {
    private final ZoneService zoneService;

    public ZoneController(ZoneService zoneService) {
        this.zoneService = zoneService;
    }

    @GetMapping("/fields/{fieldId}/zones")
    public Object fieldZones(
            @PathVariable UUID fieldId,
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size) {
        if (page == null && size == null) {
            return zoneService.findFieldZones(fieldId);
        }
        return zoneService.findFieldZones(fieldId, pageRequest(page, size));
    }

    @GetMapping("/zones")
    public Object zones(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID fieldId) {
        if (page == null && size == null && fieldId == null) {
            return zoneService.findZones();
        }
        PageRequest pageable = pageRequest(page, size);
        return fieldId == null ? zoneService.findZones(pageable) : zoneService.findFieldZones(fieldId, pageable);
    }

    @PostMapping("/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneResponse createZone(@Valid @RequestBody ZoneCreateRequest request) {
        return zoneService.createZone(request);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
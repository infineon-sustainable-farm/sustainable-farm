package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.ZoneBlockMappingUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.ZoneBlockMappingService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/water/zone-block-mappings")
public class ZoneBlockMappingController {
    private final ZoneBlockMappingService mappingService;

    public ZoneBlockMappingController(ZoneBlockMappingService mappingService) {
        this.mappingService = mappingService;
    }

    @GetMapping
    public List<ZoneBlockMappingResponse> mappings(
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) String blockCode,
            @RequestParam(required = false) UUID farmId) {
        return mappingService.findAll(zoneId, blockCode, farmId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneBlockMappingResponse create(@Valid @RequestBody ZoneBlockMappingCreateRequest request) {
        return mappingService.create(request);
    }

    @GetMapping("/{mappingId}")
    public ZoneBlockMappingResponse get(@PathVariable UUID mappingId) {
        return mappingService.get(mappingId);
    }

    @PutMapping("/{mappingId}")
    public ZoneBlockMappingResponse update(@PathVariable UUID mappingId,
                                           @RequestBody ZoneBlockMappingUpdateRequest request) {
        return mappingService.update(mappingId, request);
    }

    @DeleteMapping("/{mappingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID mappingId) {
        mappingService.delete(mappingId);
    }
}

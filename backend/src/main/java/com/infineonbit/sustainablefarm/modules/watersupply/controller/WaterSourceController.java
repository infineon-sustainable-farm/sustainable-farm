package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterSourceService;
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
@RequestMapping("/api/water/sources")
public class WaterSourceController {
    private final WaterSourceService waterSourceService;

    public WaterSourceController(WaterSourceService waterSourceService) {
        this.waterSourceService = waterSourceService;
    }

    @GetMapping
    public Object sources(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID farmId) {
        if (page == null && size == null && farmId == null) {
            return waterSourceService.findSources();
        }
        return waterSourceService.findSources(pageRequest(page, size), farmId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaterSourceResponse createSource(@Valid @RequestBody WaterSourceCreateRequest request) {
        return waterSourceService.createSource(request);
    }

    @GetMapping("/{sourceId}")
    public WaterSourceResponse source(@PathVariable UUID sourceId) {
        return waterSourceService.getSource(sourceId);
    }

    @PutMapping("/{sourceId}")
    public WaterSourceResponse updateSource(@PathVariable UUID sourceId, @RequestBody WaterSourceUpdateRequest request) {
        return waterSourceService.updateSource(sourceId, request);
    }

    @DeleteMapping("/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(@PathVariable UUID sourceId) {
        waterSourceService.deleteSource(sourceId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
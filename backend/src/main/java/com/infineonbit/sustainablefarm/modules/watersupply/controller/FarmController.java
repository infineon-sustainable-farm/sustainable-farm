package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.FarmUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.FarmService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
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
@RequestMapping("/api/farms")
public class FarmController {
    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @GetMapping
    public Object farms(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size) {
        if (page == null && size == null) {
            return farmService.findFarms();
        }
        return farmService.findFarms(pageRequest(page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FarmResponse createFarm(@Valid @RequestBody FarmCreateRequest request) {
        return farmService.createFarm(request);
    }

    @GetMapping("/{farmId}")
    public FarmResponse farm(@PathVariable UUID farmId) {
        return farmService.getFarm(farmId);
    }

    @PutMapping("/{farmId}")
    public FarmResponse updateFarm(@PathVariable UUID farmId, @RequestBody FarmUpdateRequest request) {
        return farmService.updateFarm(farmId, request);
    }

    @DeleteMapping("/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFarm(@PathVariable UUID farmId) {
        farmService.deleteFarm(farmId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}
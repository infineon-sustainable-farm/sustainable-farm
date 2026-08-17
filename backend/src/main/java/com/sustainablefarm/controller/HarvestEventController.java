package com.sustainablefarm.controller;

import com.sustainablefarm.dto.mapper.DtoMapper;
import com.sustainablefarm.dto.request.HarvestEventCreateRequest;
import com.sustainablefarm.dto.request.HarvestEventUpdateRequest;
import com.sustainablefarm.dto.response.HarvestEventResponse;
import com.sustainablefarm.dto.response.PageResponse;
import com.sustainablefarm.model.HarvestEvent;
import com.sustainablefarm.model.HarvestEvent.MangoVariety;
import com.sustainablefarm.model.HarvestEvent.QualityGrade;
import com.sustainablefarm.service.HarvestEventService;
import com.sustainablefarm.util.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/harvest-events")
@Tag(name = "Harvest Integration", description = "APIs for harvest events from the Plants workstream")
public class HarvestEventController {

    private final HarvestEventService harvestEventService;
    private final DtoMapper dtoMapper;

    @Autowired
    public HarvestEventController(HarvestEventService harvestEventService, DtoMapper dtoMapper) {
        this.harvestEventService = harvestEventService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping
    @Operation(summary = "Create harvest event")
    public ResponseEntity<HarvestEventResponse> createHarvestEvent(
            @Valid @RequestBody HarvestEventCreateRequest request) {
        HarvestEvent event = dtoMapper.toEntity(request);
        HarvestEvent created = harvestEventService.createHarvestEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(created));
    }

    @PostMapping("/sync")
    @Operation(summary = "Sync harvest event from Plants workstream")
    public ResponseEntity<HarvestEventResponse> syncFromPlants(
            @Valid @RequestBody HarvestEventCreateRequest request) {
        HarvestEvent event = dtoMapper.toEntity(request);
        HarvestEvent synced = harvestEventService.syncFromPlants(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toResponse(synced));
    }

    @GetMapping("/{harvestId}")
    @Operation(summary = "Get harvest event by ID")
    public ResponseEntity<HarvestEventResponse> getHarvestEventById(@PathVariable String harvestId) {
        return ResponseEntity.ok(dtoMapper.toResponse(harvestEventService.getHarvestEventById(harvestId)));
    }

    @GetMapping("/batch/{batchId}")
    @Operation(summary = "Get harvest event by batch ID")
    public ResponseEntity<HarvestEventResponse> getHarvestEventByBatchId(@PathVariable String batchId) {
        return ResponseEntity.ok(dtoMapper.toResponse(harvestEventService.getHarvestEventByBatchId(batchId)));
    }

    @GetMapping
    @Operation(summary = "Get all harvest events (paginated)")
    public ResponseEntity<PageResponse<HarvestEventResponse>> getAllHarvestEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<HarvestEventResponse> responses = harvestEventService.getAllHarvestEvents().stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(PaginationUtils.paginate(responses, page, size));
    }

    @PutMapping("/{harvestId}")
    @Operation(summary = "Update harvest event")
    public ResponseEntity<HarvestEventResponse> updateHarvestEvent(
            @PathVariable String harvestId,
            @Valid @RequestBody HarvestEventUpdateRequest request) {
        HarvestEvent existing = harvestEventService.getHarvestEventById(harvestId);
        applyUpdate(existing, request);
        HarvestEvent updated = harvestEventService.updateHarvestEvent(harvestId, existing);
        return ResponseEntity.ok(dtoMapper.toResponse(updated));
    }

    @DeleteMapping("/{harvestId}")
    @Operation(summary = "Delete harvest event")
    public ResponseEntity<Void> deleteHarvestEvent(@PathVariable String harvestId) {
        harvestEventService.deleteHarvestEvent(harvestId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get harvest events by date range")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        List<HarvestEventResponse> responses = harvestEventService
                .getHarvestEventsByDateRange(startDate, endDate).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/variety/{variety}")
    @Operation(summary = "Get harvest events by mango variety")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByVariety(@PathVariable MangoVariety variety) {
        List<HarvestEventResponse> responses = harvestEventService.getHarvestEventsByVariety(variety).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/grade/{grade}")
    @Operation(summary = "Get harvest events by quality grade")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByQualityGrade(@PathVariable QualityGrade grade) {
        List<HarvestEventResponse> responses = harvestEventService.getHarvestEventsByQualityGrade(grade).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/farm/{farmId}")
    @Operation(summary = "Get harvest events by farm")
    public ResponseEntity<List<HarvestEventResponse>> getHarvestEventsByFarm(@PathVariable String farmId) {
        List<HarvestEventResponse> responses = harvestEventService.getHarvestEventsByFarm(farmId).stream()
                .map(dtoMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/batch-id/{batchId}/exists")
    @Operation(summary = "Check if batch ID already exists")
    public ResponseEntity<Boolean> batchIdExists(@PathVariable String batchId) {
        return ResponseEntity.ok(harvestEventService.batchIdExists(batchId));
    }

    private void applyUpdate(HarvestEvent existing, HarvestEventUpdateRequest request) {
        if (request.getHarvestDate() != null) {
            existing.setHarvestDate(request.getHarvestDate());
        }
        if (request.getHarvestTime() != null) {
            existing.setHarvestTime(request.getHarvestTime());
        }
        if (request.getMangoVariety() != null) {
            existing.setMangoVariety(request.getMangoVariety());
        }
        if (request.getFarmId() != null) {
            existing.setFarmId(request.getFarmId());
        }
        if (request.getBlockId() != null) {
            existing.setBlockId(request.getBlockId());
        }
        if (request.getHarvestQuantityKg() != null) {
            existing.setHarvestQuantityKg(request.getHarvestQuantityKg());
        }
        if (request.getQualityGrade() != null) {
            existing.setQualityGrade(request.getQualityGrade());
        }
        if (request.getQualityGradeDescription() != null) {
            existing.setQualityGradeDescription(request.getQualityGradeDescription());
        }
        if (request.getHarvestTeamId() != null) {
            existing.setHarvestTeamId(request.getHarvestTeamId());
        }
        if (request.getHarvestSupervisor() != null) {
            existing.setHarvestSupervisor(request.getHarvestSupervisor());
        }
        if (request.getWeatherConditions() != null) {
            existing.setWeatherConditions(request.getWeatherConditions());
        }
        if (request.getStorageLocation() != null) {
            existing.setStorageLocation(request.getStorageLocation());
        }
    }
}

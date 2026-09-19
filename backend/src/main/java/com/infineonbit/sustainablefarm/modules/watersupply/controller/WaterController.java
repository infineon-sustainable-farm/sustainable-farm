package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterConsumptionUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterQualityUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceCreateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterSourceUpdateRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterService;
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
@RequestMapping("/api/water")
public class WaterController {
    private final WaterService waterService;

    public WaterController(WaterService waterService) {
        this.waterService = waterService;
    }

    @GetMapping("/sources")
    public Object sources(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID farmId) {
        if (page == null && size == null && farmId == null) return waterService.findSources();
        return waterService.findSources(pageRequest(page, size), farmId);
    }

    @PostMapping("/sources")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterSourceResponse createSource(@Valid @RequestBody WaterSourceCreateRequest request) {
        return waterService.createSource(request);
    }

    @GetMapping("/sources/{sourceId}")
    public WaterSourceResponse source(@PathVariable UUID sourceId) {
        return waterService.getSource(sourceId);
    }

    @PutMapping("/sources/{sourceId}")
    public WaterSourceResponse updateSource(@PathVariable UUID sourceId, @RequestBody WaterSourceUpdateRequest request) {
        return waterService.updateSource(sourceId, request);
    }

    @DeleteMapping("/sources/{sourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSource(@PathVariable UUID sourceId) {
        waterService.deleteSource(sourceId);
    }

    @GetMapping("/consumption")
    public Object consumption(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID farmId,
            @RequestParam(required = false) UUID sourceId) {
        if (page == null && size == null && farmId == null && sourceId == null) return waterService.findConsumptions();
        return waterService.findConsumptions(pageRequest(page, size), farmId, sourceId);
    }

    @PostMapping("/consumption")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterConsumptionResponse createConsumption(@Valid @RequestBody WaterConsumptionCreateRequest request) {
        return waterService.createConsumption(request);
    }

    @GetMapping("/consumption/{consumptionId}")
    public WaterConsumptionResponse consumption(@PathVariable UUID consumptionId) {
        return waterService.getConsumption(consumptionId);
    }

    @PutMapping("/consumption/{consumptionId}")
    public WaterConsumptionResponse updateConsumption(@PathVariable UUID consumptionId, @RequestBody WaterConsumptionUpdateRequest request) {
        return waterService.updateConsumption(consumptionId, request);
    }

    @DeleteMapping("/consumption/{consumptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConsumption(@PathVariable UUID consumptionId) {
        waterService.deleteConsumption(consumptionId);
    }

    @GetMapping("/quality")
    public Object quality(
            @RequestParam(required = false) @Min(0) Integer page,
            @RequestParam(required = false) @Min(1) Integer size,
            @RequestParam(required = false) UUID sourceId) {
        if (page == null && size == null && sourceId == null) return waterService.findQualityTests();
        return waterService.findQualityTests(pageRequest(page, size), sourceId);
    }

    /**
     * Cree un test qualite et genere automatiquement une Notification de type
     * "warning" si un parametre sort des plages cibles (pH hors [6.0, 7.5],
     * turbidite > 5 NTU.
     */
    @PostMapping("/quality")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterQualityResponse createQualityTest(@Valid @RequestBody WaterQualityCreateRequest request) {
        return waterService.createQualityTest(request);
    }

    @GetMapping("/quality/{qualityTestId}")
    public WaterQualityResponse qualityTest(@PathVariable UUID qualityTestId) {
        return waterService.getQualityTest(qualityTestId);
    }

    @PutMapping("/quality/{qualityTestId}")
    public WaterQualityResponse updateQualityTest(@PathVariable UUID qualityTestId, @RequestBody WaterQualityUpdateRequest request) {
        return waterService.updateQualityTest(qualityTestId, request);
    }

    @DeleteMapping("/quality/{qualityTestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQualityTest(@PathVariable UUID qualityTestId) {
        waterService.deleteQualityTest(qualityTestId);
    }

    private PageRequest pageRequest(Integer page, Integer size) {
        return PageRequest.of(page == null ? 0 : page, size == null ? 20 : size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
    }
}

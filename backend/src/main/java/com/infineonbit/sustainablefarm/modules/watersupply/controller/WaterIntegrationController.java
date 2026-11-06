package com.infineonbit.sustainablefarm.modules.watersupply.controller;

import com.infineonbit.sustainablefarm.modules.watersupply.dto.AvailabilityForecastRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.BlockIrrigationNeedRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.LotUsageRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.SafetyBriefingRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.VisitorEventRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.WaterUnavailabilityResponse;
import com.infineonbit.sustainablefarm.modules.watersupply.service.IrrigationService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.PlantsIntegrationService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.ProductTransformationIntegrationService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.VisitorIntegrationService;
import com.infineonbit.sustainablefarm.modules.watersupply.service.WaterUnavailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints consumed by the other modules (Plants, Visitor Management, Product Transformation) and
 * planned water unavailability. Contracts and examples: {@code docs/integrations/}.
 * Device commands are in {@code iot.IotCommandController}.
 */
@RestController
@RequestMapping("/api/water")
@Tag(name = "Water Supply - Integrations", description = "Read models and events for Plants, Visitor Management and Product Transformation")
public class WaterIntegrationController {
    private final PlantsIntegrationService plantsService;
    private final VisitorIntegrationService visitorService;
    private final ProductTransformationIntegrationService ptService;
    private final IrrigationService irrigationService;
    private final WaterUnavailabilityService unavailabilityService;

    public WaterIntegrationController(PlantsIntegrationService plantsService, VisitorIntegrationService visitorService,
                                      ProductTransformationIntegrationService ptService, IrrigationService irrigationService,
                                      WaterUnavailabilityService unavailabilityService) {
        this.plantsService = plantsService;
        this.visitorService = visitorService;
        this.ptService = ptService;
        this.irrigationService = irrigationService;
        this.unavailabilityService = unavailabilityService;
    }

    // --- Plants ---------------------------------------------------------------------------------

    @GetMapping("/plants/rainfall")
    @Operation(summary = "Rain gauge rainfall of a Plants block over [from, to] (days, UTC)")
    public Map<String, Object> rainfall(@RequestParam String blockCode,
                                        @RequestParam(required = false) UUID farmId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return plantsService.rainfall(blockCode, farmId, from, to);
    }

    @PostMapping("/plants/irrigation-need")
    @Operation(summary = "Irrigation need of a Plants block (FAO-56 x growth phase factor)")
    public Map<String, Object> irrigationNeed(@Valid @RequestBody BlockIrrigationNeedRequest request) {
        return plantsService.irrigationNeed(request);
    }

    // --- Visitor Management ---------------------------------------------------------------------

    @GetMapping("/visitor/irrigation-status")
    @Operation(summary = "Is the zone being irrigated, and when was it last activated")
    public Map<String, Object> irrigationStatus(@RequestParam UUID zoneId) {
        return irrigationService.irrigationStatus(zoneId);
    }

    @GetMapping("/visitor/live-consumption")
    @Operation(summary = "Volume measured over the last minutes, for a source, a zone or the whole site")
    public Map<String, Object> liveConsumption(@RequestParam(required = false) UUID sourceId,
                                               @RequestParam(required = false) UUID zoneId,
                                               @RequestParam(required = false) Integer windowMinutes) {
        return visitorService.liveConsumption(sourceId, zoneId, windowMinutes);
    }

    @GetMapping("/visitor/summary")
    @Operation(summary = "Level, savings, irrigation status, live consumption and unavailability in one call")
    public Map<String, Object> visitorSummary(@RequestParam(required = false) UUID sourceId,
                                              @RequestParam(required = false) UUID zoneId) {
        return visitorService.summary(sourceId, zoneId);
    }

    @PostMapping("/visitor-events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Visit announcement from Visitor Management (idempotent on visitId)")
    public Map<String, Object> visitorEvent(@Valid @RequestBody VisitorEventRequest request) {
        return visitorService.visitorEvent(request);
    }

    @GetMapping("/visitor/safety-briefing")
    @Operation(summary = "Current water safety briefing for visitors")
    public Map<String, Object> safetyBriefing() {
        return visitorService.safetyBriefing();
    }

    @PatchMapping("/visitor/safety-briefing")
    @Operation(summary = "Publish a new version of the safety briefing (omitted fields are kept)")
    public Map<String, Object> updateSafetyBriefing(@RequestBody SafetyBriefingRequest request) {
        return visitorService.updateSafetyBriefing(request);
    }

    // --- Planned water unavailability -----------------------------------------------------------

    @GetMapping("/outages")
    @Operation(summary = "Planned unavailability, filtered by period, zone/source (farm-wide included) or instant")
    public List<WaterUnavailabilityResponse> outages(@RequestParam(required = false) Instant from,
                                                     @RequestParam(required = false) Instant to,
                                                     @RequestParam(required = false) UUID zoneId,
                                                     @RequestParam(required = false) UUID sourceId,
                                                     @RequestParam(required = false) Instant activeOn) {
        return unavailabilityService.search(from, to, zoneId, sourceId, activeOn);
    }

    @PostMapping("/outages")
    @ResponseStatus(HttpStatus.CREATED)
    public WaterUnavailabilityResponse createOutage(@Valid @RequestBody WaterUnavailabilityRequest request) {
        return unavailabilityService.create(request);
    }

    @GetMapping("/outages/{outageId}")
    public WaterUnavailabilityResponse outage(@PathVariable UUID outageId) {
        return unavailabilityService.get(outageId);
    }

    @PutMapping("/outages/{outageId}")
    public WaterUnavailabilityResponse updateOutage(@PathVariable UUID outageId,
                                                    @Valid @RequestBody WaterUnavailabilityRequest request) {
        return unavailabilityService.update(outageId, request);
    }

    @DeleteMapping("/outages/{outageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOutage(@PathVariable UUID outageId) {
        unavailabilityService.delete(outageId);
    }

    // --- Product Transformation -----------------------------------------------------------------

    @PostMapping("/lot-usage")
    @Operation(summary = "Attribute the consumptions of a time window to a lot and activity")
    public Map<String, Object> lotUsage(@Valid @RequestBody LotUsageRequest request) {
        return ptService.attachLotUsage(request);
    }

    @GetMapping("/consumption/by-lot")
    @Operation(summary = "Water consumed by a lot, per activity")
    public Map<String, Object> consumptionByLot(@RequestParam String lotId) {
        return ptService.byLot(lotId);
    }

    @GetMapping("/quality/compliance")
    @Operation(summary = "Water quality verdict for a lot (its sources and window) or for a period")
    public Map<String, Object> qualityCompliance(@RequestParam(required = false) String lotId,
                                                 @RequestParam(required = false) Instant from,
                                                 @RequestParam(required = false) Instant to) {
        return ptService.qualityCompliance(lotId, from, to);
    }

    @PostMapping("/product-transformation/availability-forecast")
    @Operation(summary = "Source availability after planned batches and irrigations, with buffer status")
    public Map<String, Object> availabilityForecast(@Valid @RequestBody AvailabilityForecastRequest request) {
        return ptService.forecast(request);
    }
}

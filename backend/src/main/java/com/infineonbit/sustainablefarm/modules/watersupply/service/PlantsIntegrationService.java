package com.infineonbit.sustainablefarm.modules.watersupply.service;

import com.infineonbit.sustainablefarm.modules.watersupply.config.WaterIntegrationProperties;
import com.infineonbit.sustainablefarm.modules.watersupply.dto.BlockIrrigationNeedRequest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.RainwaterHarvest;
import com.infineonbit.sustainablefarm.modules.watersupply.entity.Zone;
import com.infineonbit.sustainablefarm.modules.watersupply.exception.NotFoundException;
import com.infineonbit.sustainablefarm.modules.watersupply.repository.FieldZoneRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plants integration: rainfall and irrigation need expressed per Plants block ({@code blockCode})
 * instead of per Water Supply zone, through {@code zone_block_mapping}.
 *
 * <p>Irrigation need = FAO-56 need of the mapped zones (area x ET0 x Kc / efficiency, see
 * {@link WaterNeedService}) x a growth phase factor. The factors are configuration
 * ({@code app.water.plants.phase-factor.*}) and default to 1.0, i.e. no adjustment, until the
 * agronomists validate them: inventing coefficients would make the need look precise while it is not.</p>
 */
@Service
@Transactional(readOnly = true)
public class PlantsIntegrationService {
    static final String ESTABLISHMENT = "establishment";
    static final String GRADUAL_PRODUCTION = "gradual production";
    static final String FULL_PRODUCTION = "full production";

    /** Growth phase applied to a zone and where it comes from (request, plants_calendar or unknown). */
    public record PhaseAdjustment(String growthPhase, double factor, String source) {
        static final PhaseAdjustment NONE = new PhaseAdjustment(null, 1.0, "unknown");
    }

    private final ZoneBlockMappingService zoneBlockMappingService;
    private final FieldZoneRepository zoneRepository;
    private final RainfallService rainfallService;
    private final WaterNeedService waterNeedService;
    private final AgroWeatherService agroWeatherService;
    private final PlantsGrowthPort plantsGrowthPort;
    private final WaterIntegrationProperties properties;

    public PlantsIntegrationService(ZoneBlockMappingService zoneBlockMappingService, FieldZoneRepository zoneRepository,
                                    RainfallService rainfallService, WaterNeedService waterNeedService,
                                    AgroWeatherService agroWeatherService, PlantsGrowthPort plantsGrowthPort,
                                    WaterIntegrationProperties properties) {
        this.zoneBlockMappingService = zoneBlockMappingService;
        this.zoneRepository = zoneRepository;
        this.rainfallService = rainfallService;
        this.waterNeedService = waterNeedService;
        this.agroWeatherService = agroWeatherService;
        this.plantsGrowthPort = plantsGrowthPort;
        this.properties = properties;
    }

    /**
     * Rain gauge rainfall for a block (P2), to replace the {@code localRainfallMm} Plants keeps null.
     * The gauges are attached to sources, hence to farms: the resolution is the farm owning the block.
     *
     * @param from first day included (UTC)
     * @param to   last day included (UTC)
     */
    public Map<String, Object> rainfall(String blockCode, UUID farmId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from");
        }
        String block = ZoneBlockMappingService.normalizeBlockCode(blockCode);
        List<UUID> farmIds = zoneBlockMappingService.farmIdsForBlock(farmId, block);
        if (farmIds.isEmpty()) {
            throw new NotFoundException("Block mapping");
        }
        Instant start = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        RainfallService.RainfallSummary summary = rainfallService.forFarms(farmIds, start, end);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("blockCode", block);
        response.put("from", from);
        response.put("to", to);
        response.put("rainfallMm", round1(summary.rainfallMm()));
        response.put("source", "rain_gauge");
        response.put("spatialResolution", "farm");
        response.put("aggregation", "max_cumulative_per_gauge");
        response.put("farmIds", farmIds);
        response.put("gaugeCount", summary.bySource().size());
        response.put("readings", summary.readings().stream().map(this::rainReading).toList());
        return response;
    }

    /** Irrigation need of a block (P3) for one day. */
    public Map<String, Object> irrigationNeed(BlockIrrigationNeedRequest request) {
        String block = ZoneBlockMappingService.normalizeBlockCode(request.blockCode());
        List<UUID> zoneIds = zoneBlockMappingService.zoneIdsForBlock(request.farmId(), block);
        if (zoneIds.isEmpty()) {
            throw new NotFoundException("Block mapping");
        }
        LocalDate date = request.date() == null ? LocalDate.now(ZoneOffset.UTC) : request.date();
        PhaseAdjustment phase = request.growthPhase() == null || request.growthPhase().isBlank()
                ? phaseFromPlants(block)
                : fromRequest(request.growthPhase());
        double et0 = agroWeatherService.et0Between(date, date);
        double baseNeed = zoneIds.stream()
                .map(zoneRepository::findById)
                .flatMap(java.util.Optional::stream)
                .mapToDouble(zone -> waterNeedService.needLiters(zone, et0))
                .sum();

        List<String> warnings = new ArrayList<>();
        if ("unknown".equals(phase.source())) {
            warnings.add("growth_phase_unknown_no_adjustment");
        }
        if (!phaseFactorsConfigured()) {
            warnings.add("phase_factors_not_validated");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("blockCode", block);
        response.put("date", date);
        response.put("et0Mm", Math.round(et0 * 100d) / 100d);
        response.put("baseNeedLiters", Math.round(baseNeed));
        response.put("growthPhase", phase.growthPhase());
        response.put("growthPhaseSource", phase.source());
        response.put("phaseFactor", phase.factor());
        response.put("needLiters", Math.round(baseNeed * phase.factor()));
        response.put("treeCount", request.treeCount());
        response.put("ageYears", request.ageYears());
        response.put("method", "fao56_zone_need_x_growth_phase_factor");
        response.put("matchedZones", zoneIds);
        response.put("warnings", warnings);
        return response;
    }

    /**
     * Phase adjustment of a zone for the automatic irrigation (2.4): applied only when the zone maps
     * to exactly one block and Plants reports a single phase for it; otherwise factor 1.0.
     */
    public PhaseAdjustment phaseAdjustmentForZone(UUID zoneId) {
        List<String> blocks = zoneBlockMappingService.blockCodesForZone(zoneId);
        return blocks.size() == 1 ? phaseFromPlants(blocks.get(0)) : PhaseAdjustment.NONE;
    }

    /** Automatic irrigation volume of a zone: FAO-56 need x phase factor. */
    double needLiters(Zone zone, double et0Mm, PhaseAdjustment adjustment) {
        return waterNeedService.needLiters(zone, et0Mm) * adjustment.factor();
    }

    private PhaseAdjustment fromRequest(String growthPhase) {
        String phase = requirePhase(growthPhase);
        return new PhaseAdjustment(phase, phaseFactor(phase), "request");
    }

    private PhaseAdjustment phaseFromPlants(String blockCode) {
        List<String> phases = plantsGrowthPort.growthForBlock(blockCode).stream()
                .map(PlantsGrowthPort.BlockGrowth::growthPhase)
                .filter(Objects::nonNull)
                .map(PlantsIntegrationService::normalizePhase)
                .distinct()
                .toList();
        // Several plantings in different phases: no single factor is right, so none is applied.
        if (phases.size() != 1 || !isKnownPhase(phases.get(0))) {
            return PhaseAdjustment.NONE;
        }
        return new PhaseAdjustment(phases.get(0), phaseFactor(phases.get(0)), "plants_calendar");
    }

    private double phaseFactor(String phase) {
        return switch (phase) {
            case ESTABLISHMENT -> properties.plantsEstablishmentFactor;
            case GRADUAL_PRODUCTION -> properties.plantsGradualProductionFactor;
            case FULL_PRODUCTION -> properties.plantsFullProductionFactor;
            default -> 1.0;
        };
    }

    private boolean phaseFactorsConfigured() {
        return properties.plantsEstablishmentFactor != 1.0 || properties.plantsGradualProductionFactor != 1.0
                || properties.plantsFullProductionFactor != 1.0;
    }

    private static String requirePhase(String phase) {
        String normalized = normalizePhase(phase);
        if (!isKnownPhase(normalized)) {
            throw new IllegalArgumentException(
                    "growthPhase must be one of: " + ESTABLISHMENT + ", " + GRADUAL_PRODUCTION + ", " + FULL_PRODUCTION);
        }
        return normalized;
    }

    /** Plants sends "gradual production"; "GRADUAL_PRODUCTION" or "gradual-production" are accepted too. */
    static String normalizePhase(String phase) {
        return phase.trim().toLowerCase(Locale.ROOT).replace('_', ' ').replace('-', ' ');
    }

    private static boolean isKnownPhase(String phase) {
        return ESTABLISHMENT.equals(phase) || GRADUAL_PRODUCTION.equals(phase) || FULL_PRODUCTION.equals(phase);
    }

    private Map<String, Object> rainReading(RainwaterHarvest harvest) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("sourceId", harvest.getSourceId());
        item.put("captureDate", harvest.getCaptureDate());
        item.put("rainfallMm", harvest.getRainfallMm());
        return item;
    }

    private static double round1(double value) {
        return Math.round(value * 10d) / 10d;
    }
}

package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads the default agronomic reference at startup: the yield per tree and the
 * harvest season of each known variety, and the share of that yield given in
 * each growth phase.
 *
 * <p>Runs in every profile, unlike {@code PlantsDataSeeder}. That seeder loads
 * farm data, the Zalka 2025 orchard, which must never reach production. This
 * class loads a sourced catalogue that belongs to no farm: the yield forecast
 * needs it in every environment, production included, and without it every
 * planted variety would come back without a reference.
 *
 * <p>A row is inserted only when it is missing: a variety whose name matches,
 * ignoring case, accents and surrounding spaces (see
 * {@link VarietyReferenceMatcher}), or a growth phase with the same label. An
 * existing row is never overwritten, even when its value differs from the
 * default below. The values are defaults for the mentors to validate, and a
 * value changed in the database is the one that counts. Restarting the
 * application therefore never duplicates nor resets a row.
 *
 * <p>The lists below are the only place in the code where a yield, a share or
 * a harvest month appears. The forecast reads them from the tables.
 */
@Component
public class AgronomicReferenceLoader implements CommandLineRunner {

    /** Default yield and harvest season of a variety, with their sources. */
    private record VarietyDefault(String varietyName, double yieldPerTreeKg, String yieldSource,
                                  int harvestStartMonth, int harvestEndMonth, String seasonSource) {
    }

    /** Default yield share of a growth phase, with its source. */
    private record PhaseShareDefault(String growthPhase, double yieldShare, String source) {
    }

    private static final List<VarietyDefault> VARIETY_DEFAULTS = List.of(
            new VarietyDefault("Keitt", 220.0, "Zalka_2025", 5, 7, "varietal_guide_west_africa"),
            new VarietyDefault("Kent", 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina"),
            new VarietyDefault("Amelie", 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina"));

    /** One row per phase label of {@link GrowthPhaseCalculator}, which the forecast looks up. */
    private static final List<PhaseShareDefault> PHASE_SHARE_DEFAULTS = List.of(
            new PhaseShareDefault(GrowthPhaseCalculator.ESTABLISHMENT, 0.0, "orchard_literature"),
            new PhaseShareDefault(GrowthPhaseCalculator.GRADUAL_PRODUCTION, 0.5, "assumption_to_validate"),
            new PhaseShareDefault(GrowthPhaseCalculator.FULL_PRODUCTION, 1.0, "by_definition"));

    private final VarietyReferenceRepository varietyReferenceRepository;
    private final GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    public AgronomicReferenceLoader(VarietyReferenceRepository varietyReferenceRepository,
                                    GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository) {
        this.varietyReferenceRepository = varietyReferenceRepository;
        this.growthPhaseYieldShareRepository = growthPhaseYieldShareRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        load(Instant.now());
    }

    /**
     * Inserts the missing default rows, at an explicit write time so the
     * {@code lastUpdated} values can be tested.
     *
     * @param now insertion time, stored as {@code lastUpdated} of each inserted row
     */
    void load(Instant now) {
        List<VarietyReference> references = varietyReferenceRepository.findAll();
        for (VarietyDefault variety : VARIETY_DEFAULTS) {
            if (VarietyReferenceMatcher.find(variety.varietyName(), references).isEmpty()) {
                varietyReferenceRepository.save(new VarietyReference(
                        null,
                        variety.varietyName(),
                        variety.yieldPerTreeKg(),
                        variety.yieldSource(),
                        variety.harvestStartMonth(),
                        variety.harvestEndMonth(),
                        variety.seasonSource(),
                        now));
            }
        }

        Set<String> phases = growthPhaseYieldShareRepository.findAll().stream()
                .map(GrowthPhaseYieldShare::getGrowthPhase)
                .collect(Collectors.toSet());
        for (PhaseShareDefault phase : PHASE_SHARE_DEFAULTS) {
            if (!phases.contains(phase.growthPhase())) {
                growthPhaseYieldShareRepository.save(new GrowthPhaseYieldShare(
                        null,
                        phase.growthPhase(),
                        phase.yieldShare(),
                        phase.source(),
                        now));
            }
        }
    }
}

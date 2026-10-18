package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Loads the default reference of the known varieties at startup in the dev
 * profile: the yield per tree and the harvest season of each, with their
 * sources, so a local database gives a yield forecast from the start.
 *
 * <p>Runs in the dev profile only, like {@link CurrencyRateLoader}: no
 * reference data is loaded in production. There, the user enters each variety
 * with {@code POST /api/plants/variety-references}; until then, a planted
 * variety is listed apart by the forecast.
 *
 * <p>A variety is inserted only when its name is missing, ignoring case,
 * accents and surrounding spaces (see {@link VarietyReferenceMatcher}). An
 * existing row is never overwritten, even when its value differs from the
 * default below: a value corrected since is the one that counts. Restarting
 * the application therefore never duplicates nor resets a row.
 *
 * <p>The yield shares of the growth phases are not loaded: each phase has a
 * default in {@link GrowthPhaseYieldShareService}, and a row of
 * {@code growth_phase_yield_share} is a correction of it. Earlier versions of
 * this class wrote a row for each phase; the rows still holding exactly those
 * values are removed, so that the defaults apply, gradual production at 0.25
 * instead of 0.5. A share the user corrected stays.
 */
@Component
@Profile("dev")
public class AgronomicReferenceLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AgronomicReferenceLoader.class);

    /** Default yield and harvest season of a variety, with their sources. */
    private record VarietyDefault(String varietyName, double yieldPerTreeKg, String yieldSource,
                                  int harvestStartMonth, int harvestEndMonth, String seasonSource) {
    }

    /** A share row that earlier versions of this class wrote at startup. */
    private record FormerShareRow(String growthPhase, double yieldShare, String source) {
    }

    private static final List<VarietyDefault> VARIETY_DEFAULTS = List.of(
            new VarietyDefault("Keitt", 220.0, "Zalka_2025", 5, 7, "varietal_guide_west_africa"),
            new VarietyDefault("Kent", 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina"),
            new VarietyDefault("Amelie", 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina"));

    private static final List<FormerShareRow> FORMER_SHARE_ROWS = List.of(
            new FormerShareRow(GrowthPhaseCalculator.ESTABLISHMENT, 0.0, "orchard_literature"),
            new FormerShareRow(GrowthPhaseCalculator.GRADUAL_PRODUCTION, 0.5, "assumption_to_validate"),
            new FormerShareRow(GrowthPhaseCalculator.FULL_PRODUCTION, 1.0, "by_definition"));

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
     * Inserts the missing varieties, at an explicit write time so the
     * {@code lastUpdated} values can be tested, then removes the share rows
     * of earlier versions.
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
        removeFormerShareRows();
    }

    /**
     * Removes each share row that holds exactly what an earlier version wrote:
     * the same phase, share and source. Any other row is a correction and stays.
     */
    private void removeFormerShareRows() {
        for (GrowthPhaseYieldShare share : growthPhaseYieldShareRepository.findAll()) {
            boolean former = FORMER_SHARE_ROWS.stream()
                    .anyMatch(row -> row.growthPhase().equals(share.getGrowthPhase())
                            && row.yieldShare() == share.getYieldShare()
                            && row.source().equals(share.getSource()));
            if (former) {
                growthPhaseYieldShareRepository.delete(share);
                log.info("Removed the yield share {} ({}) of the growth phase \"{}\", written at startup by an "
                                + "earlier version; the default share of the phase applies",
                        share.getYieldShare(), share.getSource(), share.getGrowthPhase());
            }
        }
    }
}

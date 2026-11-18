package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cleans, in the dev profile, the yield shares that earlier versions wrote at
 * startup. The variety catalogue, with the agronomic reference of each
 * variety, is loaded in every profile by {@link VarietyCatalogLoader}.
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

    /** A share row that earlier versions of this class wrote at startup. */
    private record FormerShareRow(String growthPhase, double yieldShare, String source) {
    }

    private static final List<FormerShareRow> FORMER_SHARE_ROWS = List.of(
            new FormerShareRow(GrowthPhaseCalculator.ESTABLISHMENT, 0.0, "orchard_literature"),
            new FormerShareRow(GrowthPhaseCalculator.GRADUAL_PRODUCTION, 0.5, "assumption_to_validate"),
            new FormerShareRow(GrowthPhaseCalculator.FULL_PRODUCTION, 1.0, "by_definition"));

    private final GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    public AgronomicReferenceLoader(GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository) {
        this.growthPhaseYieldShareRepository = growthPhaseYieldShareRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        removeFormerShareRows();
    }

    /**
     * Removes each share row that holds exactly what an earlier version wrote:
     * the same phase, share and source. Any other row is a correction and stays.
     */
    void removeFormerShareRows() {
        for (GrowthPhaseYieldShare share : growthPhaseYieldShareRepository.findAll()) {
            boolean former = FORMER_SHARE_ROWS.stream()
                    .anyMatch(row -> row.growthPhase().equals(share.getGrowthPhase())
                            && Decimals.sameValue(row.yieldShare(), share.getYieldShare())
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

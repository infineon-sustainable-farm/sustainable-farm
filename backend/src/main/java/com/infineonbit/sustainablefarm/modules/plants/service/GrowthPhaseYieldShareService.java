package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthPhaseCalculator.GrowthPhase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The yield share of each growth phase: what share of the full-production
 * yield a tree gives at its age.
 *
 * <p>Every phase has a default, sourced, set below: the only place in the code
 * where a share appears. A row of {@code growth_phase_yield_share} is a
 * correction of that default, and the value of the row is then the one in
 * effect. No share is ever missing, whether the table is empty or not, so
 * neither the yield forecast nor the plant alerts can fail for lack of one.
 * Both read the shares on every request: a correction counts at once.
 */
@Service
@AllArgsConstructor
public class GrowthPhaseYieldShareService {

    /** Default share of a growth phase, with its source. */
    private record ShareDefault(double yieldShare, String source) {
    }

    /**
     * Default share of each phase, by phase label. The trees give no harvest
     * while they establish, a quarter of the full yield from 3 to 5 years
     * (Bally 2002), and the full yield from 6 years, by definition.
     */
    private static final Map<String, ShareDefault> SHARE_DEFAULTS = Map.of(
            GrowthPhaseCalculator.ESTABLISHMENT, new ShareDefault(0.0, "orchard_literature"),
            GrowthPhaseCalculator.GRADUAL_PRODUCTION, new ShareDefault(0.25, "Bally_2002"),
            GrowthPhaseCalculator.FULL_PRODUCTION, new ShareDefault(1.0, "by_definition"));

    private final GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    /**
     * The share of a phase as it is in effect.
     *
     * @param phase      the phase
     * @param correction its row, or {@code null} when the default applies
     * @return the API representation of the share
     */
    private static GrowthPhaseYieldShareResponse toResponse(GrowthPhase phase, GrowthPhaseYieldShare correction) {
        ShareDefault shareDefault = SHARE_DEFAULTS.get(phase.label());
        if (correction == null) {
            return new GrowthPhaseYieldShareResponse(phase.code(), phase.label(), phase.yearsBand(),
                    shareDefault.yieldShare(), shareDefault.source(), false,
                    shareDefault.yieldShare(), shareDefault.source(), null);
        }
        return new GrowthPhaseYieldShareResponse(phase.code(), phase.label(), phase.yearsBand(),
                correction.getYieldShare(), correction.getSource(), true,
                shareDefault.yieldShare(), shareDefault.source(), correction.getLastUpdated());
    }

    /**
     * Retrieves the share in effect of every growth phase.
     *
     * <p>The table holds at most one row per phase, so it is read whole. A row
     * whose label is not a phase, written by hand, is ignored.
     *
     * @return the three phases, the youngest first, each with a share
     */
    public List<GrowthPhaseYieldShareResponse> getAllShares() {
        Map<String, GrowthPhaseYieldShare> corrections = growthPhaseYieldShareRepository.findAll().stream()
                .collect(Collectors.toMap(GrowthPhaseYieldShare::getGrowthPhase, Function.identity()));
        return GrowthPhaseCalculator.phases().stream()
                .map(phase -> toResponse(phase, corrections.get(phase.label())))
                .toList();
    }
}

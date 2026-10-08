package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.GrowthPhaseYieldShareRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.exception.GrowthPhaseNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthPhaseCalculator.GrowthPhase;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The yield share of each growth phase: what share of the full-production
 * yield a tree gives at its age.
 *
 * <p>Every phase has a default, sourced, set below: the only place in the code
 * where a share appears. A row of {@code growth_phase_yield_share} is a
 * correction of that default, entered with
 * {@code PUT /api/plants/growth-phase-yield-shares/{code}}, and the value of
 * the row is then the one in effect. No share is ever missing, whether the table is empty or not, so
 * neither the yield forecast nor the plant alerts can fail for lack of one.
 * Both read the shares on every request: a correction counts at once.
 */
@Service
@AllArgsConstructor
public class GrowthPhaseYieldShareService {

    /** {@code source} of a correction entered without one. */
    static final String USER_ENTRY_SOURCE = "user_entry";

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

    /**
     * Corrects the yield share of a growth phase.
     *
     * <p>The phase is named by its code, compared without case or surrounding
     * spaces: {@code " gradual_production "} finds {@code GRADUAL_PRODUCTION}.
     * The first correction of a phase adds its row; a later one replaces it, so
     * the share, its source and its {@code lastUpdated} change together. A
     * missing or blank source is recorded as {@code user_entry}.
     *
     * @param code    the code of the phase, such as {@code "GRADUAL_PRODUCTION"}
     * @param request the share, already validated
     * @return the share as now in effect, with the default beside it
     * @throws GrowthPhaseNotFoundException if no phase has this code
     * @throws ConflictException            if another first correction of the
     *                                      phase was being recorded at the same
     *                                      time; nothing is saved
     */
    @Transactional
    public GrowthPhaseYieldShareResponse saveShare(String code, GrowthPhaseYieldShareRequest request) {
        return saveShare(code, request, Instant.now());
    }

    /**
     * Same as {@link #saveShare(String, GrowthPhaseYieldShareRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    GrowthPhaseYieldShareResponse saveShare(String code, GrowthPhaseYieldShareRequest request, Instant now) {
        List<GrowthPhase> phases = GrowthPhaseCalculator.phases();
        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        GrowthPhase phase = phases.stream()
                .filter(candidate -> candidate.code().equals(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new GrowthPhaseNotFoundException(code.trim(),
                        phases.stream().map(GrowthPhase::code).toList()));
        String source = request.source() == null || request.source().isBlank()
                ? USER_ENTRY_SOURCE
                : request.source().trim();

        Optional<GrowthPhaseYieldShare> recorded = growthPhaseYieldShareRepository.findByGrowthPhase(phase.label());
        if (recorded.isPresent()) {
            GrowthPhaseYieldShare correction = recorded.get();
            correction.setYieldShare(request.yieldShare());
            correction.setSource(source);
            correction.setLastUpdated(now);
            return toResponse(phase, growthPhaseYieldShareRepository.save(correction));
        }
        try {
            // Flushed here, so the unique constraint on the phase answers inside this method.
            return toResponse(phase, growthPhaseYieldShareRepository.saveAndFlush(
                    new GrowthPhaseYieldShare(null, phase.label(), request.yieldShare(), source, now)));
        } catch (DataIntegrityViolationException concurrentFirstCorrection) {
            throw new ConflictException("Another yield share for the " + phase.label()
                    + " phase was being recorded at the same time. Nothing was saved: please send the request again.");
        }
    }
}

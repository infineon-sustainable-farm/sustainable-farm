package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyReferenceResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyReferenceNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * The agronomic reference of the varieties, as the user enters and corrects
 * it: the yield of one tree in full production and the harvest season, each
 * with its source.
 *
 * <p>Outside the dev profile no reference is loaded at startup: a planted
 * variety stays out of the yield forecast, listed apart, until its reference
 * is entered here. The forecast and the plant alerts read the table on every
 * request, so an entry or a correction counts at once.
 *
 * <p>The reference holds a handful of rows, read whole: names are compared in
 * Java by {@link VarietyReferenceMatcher}, ignoring case, accents and
 * surrounding spaces, as for the fertilizers, because accent folding in SQL
 * would depend on the database.
 */
@Service
@AllArgsConstructor
public class VarietyReferenceService {

    /** {@code source} of a value entered without one. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    /** By name, ignoring case and accents, then by identifier: the same order on every database. */
    private static final Comparator<VarietyReference> BY_NAME = Comparator
            .comparing((VarietyReference reference) -> VarietyReferenceMatcher.matchKey(reference.getVarietyName()))
            .thenComparing(VarietyReference::getId);

    private final VarietyReferenceRepository varietyReferenceRepository;

    static VarietyReferenceResponse toResponse(VarietyReference reference) {
        return new VarietyReferenceResponse(
                reference.getId(),
                reference.getVarietyName(),
                reference.getYieldPerTreeKg(),
                reference.getYieldSource(),
                reference.getHarvestStartMonth(),
                reference.getHarvestEndMonth(),
                reference.getSeasonSource(),
                reference.getLastUpdated());
    }

    /**
     * A source as stored: trimmed, and {@code null} when it is missing or blank.
     *
     * @param source the source as received, possibly {@code null}
     * @return the trimmed source, or {@code null} if it was null or blank
     */
    private static String normalizeSource(String source) {
        return source == null || source.isBlank() ? null : source.trim();
    }

    /**
     * Refuses a name that another reference already has, ignoring case,
     * accents and surrounding spaces: "Keitt" and " keítt " are the same variety.
     *
     * @param name      the trimmed name
     * @param excludeId the reference being corrected, which may keep its own
     *                  name, or {@code null} for a new reference
     * @throws ConflictException if another reference has the name
     */
    private void refuseTakenName(String name, Long excludeId) {
        String key = VarietyReferenceMatcher.matchKey(name);
        varietyReferenceRepository.findAll().stream()
                .filter(existing -> !existing.getId().equals(excludeId))
                .filter(existing -> VarietyReferenceMatcher.matchKey(existing.getVarietyName()).equals(key))
                .min(Comparator.comparing(VarietyReference::getId))
                .ifPresent(existing -> {
                    throw new ConflictException(nameTaken(existing.getVarietyName()));
                });
    }

    private static String nameTaken(String name) {
        return "A variety reference named " + name + " already exists";
    }

    /**
     * Writes the reference and flushes it, so that the unique constraint on
     * the name answers inside the request: two identical names sent at the
     * same time keep one row, and the other request gets a 409.
     */
    private VarietyReferenceResponse saveFlushed(VarietyReference reference) {
        try {
            return toResponse(varietyReferenceRepository.saveAndFlush(reference));
        } catch (DataIntegrityViolationException sameNameAtTheSameTime) {
            // The constraint answers once the other write is committed: the name is taken by now.
            throw new ConflictException(nameTaken(reference.getVarietyName()));
        }
    }

    /**
     * Retrieves the whole reference.
     *
     * @return the references ordered by name, ignoring case and accents, then
     *         by identifier; possibly empty
     */
    public List<VarietyReferenceResponse> getAllReferences() {
        return varietyReferenceRepository.findAll().stream()
                .sorted(BY_NAME)
                .map(VarietyReferenceService::toResponse)
                .toList();
    }

    /**
     * Enters the reference of a variety.
     *
     * <p>The name is trimmed, and refused when another reference has it,
     * ignoring case, accents and surrounding spaces. A missing or blank source
     * is recorded as {@code user_entry}.
     *
     * @param request the reference, already validated
     * @return the entered reference
     * @throws ConflictException if a reference with the same name exists,
     *                           including when the same name was being entered
     *                           at the same time
     */
    @Transactional
    public VarietyReferenceResponse createReference(VarietyReferenceRequest request) {
        return createReference(request, Instant.now());
    }

    /**
     * Same as {@link #createReference(VarietyReferenceRequest)}, at an explicit
     * write time so the {@code lastUpdated} value can be tested.
     */
    VarietyReferenceResponse createReference(VarietyReferenceRequest request, Instant now) {
        String name = request.varietyName().trim();
        refuseTakenName(name, null);
        String yieldSource = normalizeSource(request.yieldSource());
        String seasonSource = normalizeSource(request.seasonSource());
        return saveFlushed(new VarietyReference(null, name, request.yieldPerTreeKg(),
                yieldSource == null ? USER_ENTRY_SOURCE : yieldSource,
                request.harvestStartMonth(), request.harvestEndMonth(),
                seasonSource == null ? USER_ENTRY_SOURCE : seasonSource,
                now));
    }

    /**
     * Corrects the reference of a variety: every value is replaced.
     *
     * <p>The name is checked as for a new reference, except against the
     * reference itself, which may change the case or the accents of its own
     * name. A source sent is recorded as sent, trimmed. A missing or blank
     * source stays as it was when its value does not change, and becomes
     * {@code user_entry} when it does: the yield for {@code yieldSource}, the
     * two harvest months for {@code seasonSource}. A corrected value is never
     * credited to the source of the value it replaced.
     *
     * @param id      the identifier of the reference
     * @param request the reference, already validated
     * @return the corrected reference
     * @throws VarietyReferenceNotFoundException if no reference has this identifier
     * @throws ConflictException                 if another reference has the name
     */
    @Transactional
    public VarietyReferenceResponse updateReference(Long id, VarietyReferenceRequest request) {
        return updateReference(id, request, Instant.now());
    }

    /**
     * Same as {@link #updateReference(Long, VarietyReferenceRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    VarietyReferenceResponse updateReference(Long id, VarietyReferenceRequest request, Instant now) {
        VarietyReference reference = varietyReferenceRepository.findById(id)
                .orElseThrow(() -> new VarietyReferenceNotFoundException(id));
        String name = request.varietyName().trim();
        refuseTakenName(name, id);

        String yieldSource = normalizeSource(request.yieldSource());
        if (yieldSource == null) {
            boolean sameYield = Objects.equals(request.yieldPerTreeKg(), reference.getYieldPerTreeKg());
            yieldSource = sameYield ? reference.getYieldSource() : USER_ENTRY_SOURCE;
        }
        String seasonSource = normalizeSource(request.seasonSource());
        if (seasonSource == null) {
            boolean sameSeason = Objects.equals(request.harvestStartMonth(), reference.getHarvestStartMonth())
                    && Objects.equals(request.harvestEndMonth(), reference.getHarvestEndMonth());
            seasonSource = sameSeason ? reference.getSeasonSource() : USER_ENTRY_SOURCE;
        }

        reference.setVarietyName(name);
        reference.setYieldPerTreeKg(request.yieldPerTreeKg());
        reference.setYieldSource(yieldSource);
        reference.setHarvestStartMonth(request.harvestStartMonth());
        reference.setHarvestEndMonth(request.harvestEndMonth());
        reference.setSeasonSource(seasonSource);
        reference.setLastUpdated(now);
        return saveFlushed(reference);
    }
}

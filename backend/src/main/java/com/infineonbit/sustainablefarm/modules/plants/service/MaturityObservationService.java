package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.core.exception.BusinessRuleException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.MaturityObservationRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.MaturityObservationResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.BlockVariety;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityObservation;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.repository.BlockVarietyRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.MaturityObservationRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class MaturityObservationService {

    /** {@code source} of every observation written through the API. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    private final BlockVarietyRepository blockVarietyRepository;
    private final PopulationEventRepository populationEventRepository;
    private final MaturityObservationRepository maturityObservationRepository;
    private final PlantsBlocks plantsBlocks;
    private final PersonDirectory personDirectory;

    /**
     * A code as stored: trimmed and upper-cased, so {@code " b "} becomes
     * {@code "B"}, as for a harvest. The request validation already refused any
     * other form.
     */
    private static String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * An optional text as stored: trimmed, and {@code null} when it is blank.
     * Also turns a blank list filter into no filter, as for the other lists of
     * the module.
     *
     * @param value the text as received, possibly {@code null}
     * @return the trimmed text, or {@code null} if it was null or blank
     */
    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Maps a stored observation to its API representation. The farm, block and
     * name are those of the variety row the observation belongs to.
     *
     * @param observation the stored observation
     * @return the API representation of that observation
     */
    private MaturityObservationResponse toResponse(MaturityObservation observation) {
        BlockVariety variety = observation.getVariety();
        BigDecimal value = observation.getMeasuredValue();
        return new MaturityObservationResponse(
                observation.getId(),
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                observation.getObservedOn(),
                observation.getMaturityStatus(),
                observation.getMethod(),
                value == null ? null : value.doubleValue(),
                value == null ? null : observation.getMethod().unit(),
                personDirectory.nameOf(observation.getObserverPersonId()),
                observation.getObserverPersonId(),
                observation.getSource(),
                observation.getLastUpdated());
    }

    /**
     * Refusal of an observation whose variety has no recorded planting on the
     * block, with the words of a harvest.
     *
     * @param varietyName the stored name when the variety row exists, the name as
     *                    sent otherwise
     * @param blockCode   normalized block code
     * @return the exception to throw, answered with a 422
     */
    private static BusinessRuleException noPlanting(String varietyName, String blockCode) {
        return new BusinessRuleException("No planting of " + varietyName + " is recorded on block " + blockCode);
    }

    /**
     * Records the maturity seen in the field on a variety planted on a block,
     * in a single transaction.
     *
     * <ol>
     *     <li>Finds the variety row of the (block, name) pair, as a harvest
     *         does: the block of the code, which must belong to the requested
     *         farm when one is given (no block is created), and the name, which
     *         ignores case but not accents.</li>
     *     <li>Refuses an observation of a variety with no recorded planting.</li>
     *     <li>Refuses an observation dated before the planting of that variety.</li>
     *     <li>Stores the observation. A variety without agronomic reference is
     *         accepted: its observations are listed, and count once a reference
     *         gives it a season. Several observations on the same day are
     *         allowed.</li>
     * </ol>
     *
     * @param request the observation, already validated
     * @return the recorded observation
     * @throws BusinessRuleException if the variety has no planting on that block,
     *                               or if the observation predates the planting
     */
    @Transactional
    public MaturityObservationResponse recordObservation(MaturityObservationRequest request) {
        return recordObservation(request, Instant.now());
    }

    /**
     * Same as {@link #recordObservation(MaturityObservationRequest)}, at an
     * explicit write time so the {@code lastUpdated} value can be tested.
     */
    MaturityObservationResponse recordObservation(MaturityObservationRequest request, Instant now) {
        String blockCode = normalizeCode(request.blockCode());
        String varietyName = request.varietyName().trim();

        // An observation designates an existing variety row: an unknown block, or a block of
        // another farm than the one requested, has no planting, and no block is created.
        BlockVariety variety = plantsBlocks.find(request.farmId(), blockCode)
                .flatMap(block -> blockVarietyRepository.findByBlockAndName(block.id(), varietyName).stream().findFirst())
                .orElse(null);
        if (variety == null) {
            throw noPlanting(varietyName, blockCode);
        }
        PopulationEvent planting = populationEventRepository
                .findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(variety.getId(), PopulationEventType.PLANTING)
                .orElseThrow(() -> noPlanting(variety.getName(), blockCode));
        if (request.observedOn().isBefore(planting.getEventDate())) {
            throw new BusinessRuleException("The observation date " + request.observedOn()
                    + " is before the planting date " + planting.getEventDate()
                    + " of " + variety.getName() + " on block " + variety.getBlockCode());
        }

        MaturityObservation observation = new MaturityObservation();
        observation.setVariety(variety);
        observation.setObservedOn(request.observedOn());
        observation.setMaturityStatus(MaturityStatus.valueOf(normalizeCode(request.maturityStatus())));
        observation.setMethod(MaturityMethod.valueOf(normalizeCode(request.method())));
        // One decimal at most, and only with a method that measures one: the request validation refused anything else.
        observation.setMeasuredValue(request.measuredValue() == null
                ? null
                : BigDecimal.valueOf(request.measuredValue()).setScale(1, RoundingMode.HALF_UP));
        observation.setObserverPersonId(personDirectory.requireExisting(request.observerPersonId()));
        observation.setSource(USER_ENTRY_SOURCE);
        observation.setLastUpdated(now);
        return toResponse(maturityObservationRepository.save(observation));
    }

    /**
     * Retrieves the observations matching the optional filters.
     *
     * <p>Every filter is independent and optional; a missing farm means every
     * farm, as for the other lists of the module. Both dates are included. A
     * filter that matches nothing, or a {@code from} after {@code to}, returns
     * an empty list and is never an error.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode block code (for example {@code "A"}), or {@code null}
     *                  or blank for every block
     * @param from      first observation date, included, or {@code null}
     * @param to        last observation date, included, or {@code null}
     * @return the matching observations, ordered by date then identifier
     */
    public List<MaturityObservationResponse> getAllObservations(Long farmId, String blockCode,
                                                                LocalDate from, LocalDate to) {
        PlantsBlocks.BlockFilter blocks = plantsBlocks.filter(farmId, blockCode);
        return maturityObservationRepository.findByOptionalFilters(blocks.all(), blocks.ids(), from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}

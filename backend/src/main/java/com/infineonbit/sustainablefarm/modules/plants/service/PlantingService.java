package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.PlantingRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.PlantingResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

@Service
@AllArgsConstructor
public class PlantingService {

    /** {@code source} of every row written from the planting form. */
    static final String USER_ENTRY_SOURCE = "user_entry";

    /** {@code datePrecision} of a date entered through the form, which is always a full date. */
    static final String DAY_KNOWN_PRECISION = "day known";

    private final VarietyRepository varietyRepository;
    private final GrowthCalendarRepository growthCalendarRepository;
    private final PopulationEventRepository populationEventRepository;

    /**
     * Block code as stored: trimmed and upper-cased, so {@code " a "} becomes
     * {@code "A"}. Prefixes and inner spaces were already refused by the
     * request validation.
     *
     * @param blockCode the block code as received
     * @return the block code as stored
     */
    private static String normalizeBlockCode(String blockCode) {
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * New variety row for a (farm, block, name) triple planted for the first time.
     *
     * <p>{@code treeCount} is the number declared when the row is created, that
     * is the trees of this planting. Spacing, density, yields, vigor and plant
     * origin are not entered with a planting: they stay NULL rather than being
     * guessed.
     */
    private static Variety newVariety(Integer farmId, String blockCode, String name, Integer treeCount, Instant now) {
        Variety variety = new Variety();
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        variety.setTreeCount(treeCount);
        variety.setSource(USER_ENTRY_SOURCE);
        variety.setLastUpdated(now);
        return variety;
    }

    /**
     * Maps a stored planting event to its API representation. The farm, block
     * and name are those of the variety row the event belongs to.
     *
     * @param planting the stored PLANTING event
     * @return the API representation of that planting
     */
    private static PlantingResponse toResponse(PopulationEvent planting) {
        Variety variety = planting.getVariety();
        return new PlantingResponse(
                planting.getId(),
                variety.getFarmId(),
                variety.getBlockCode(),
                variety.getId(),
                variety.getName(),
                planting.getEventDate(),
                planting.getTreeCount(),
                planting.getSource(),
                planting.getLastUpdated());
    }

    /**
     * Records the planting of a variety on a block, in a single transaction.
     *
     * <ol>
     *     <li>Finds the variety row of the (farm, block, name) triple. A NULL farm
     *         matches only the rows without a farm, and the name ignores case. An
     *         existing row is reused as it is and locked until the commit; a
     *         missing one is created.</li>
     *     <li>Refuses a second planting of the same variety row. The lock makes a
     *         planting sent at the same time wait, then see this one.</li>
     *     <li>Creates the growth calendar row of the block, or moves its date back
     *         when this planting is older than the recorded one. An existing
     *         calendar row is locked the same way.</li>
     *     <li>Stores the PLANTING event.</li>
     * </ol>
     *
     * @param request the planting, already validated
     * @return the recorded planting
     * @throws ConflictException if a planting is already recorded for this variety row
     */
    @Transactional
    public PlantingResponse recordPlanting(PlantingRequest request) {
        return recordPlanting(request, Instant.now());
    }

    /**
     * Same as {@link #recordPlanting(PlantingRequest)}, at an explicit write time
     * so the {@code lastUpdated} values can be tested.
     */
    PlantingResponse recordPlanting(PlantingRequest request, Instant now) {
        Integer farmId = request.farmId();
        String blockCode = normalizeBlockCode(request.blockCode());
        String varietyName = request.varietyName().trim();

        Variety variety = varietyRepository.findByFarmBlockAndNameForUpdate(farmId, blockCode, varietyName)
                .stream()
                .findFirst()
                .orElse(null);
        if (variety == null) {
            variety = varietyRepository.save(newVariety(farmId, blockCode, varietyName, request.treeCount(), now));
        } else if (populationEventRepository.existsByVarietyIdAndEventType(variety.getId(), PopulationEventType.PLANTING)) {
            throw new ConflictException("A planting of " + variety.getName()
                    + " is already recorded on block " + variety.getBlockCode());
        }

        recordPlantingDate(farmId, blockCode, request.plantingDate(), now);

        PopulationEvent planting = new PopulationEvent();
        planting.setVariety(variety);
        planting.setEventType(PopulationEventType.PLANTING);
        planting.setEventDate(request.plantingDate());
        planting.setTreeCount(request.treeCount());
        planting.setSource(USER_ENTRY_SOURCE);
        planting.setLastUpdated(now);
        return toResponse(populationEventRepository.save(planting));
    }

    /**
     * Brings the growth calendar of the block up to date with a new planting.
     *
     * <p>The calendar date is that of the oldest planting of the block. A block
     * without a calendar row gets one. An existing row takes the new date only
     * when its own date is NULL or later; its precision and source then follow
     * the new date. When its date is the same or older, the row is left untouched.
     *
     * @param farmId       farm identifier, or {@code null} for no farm
     * @param blockCode    normalized block code
     * @param plantingDate date of the planting being recorded
     * @param now          write time
     */
    private void recordPlantingDate(Integer farmId, String blockCode, LocalDate plantingDate, Instant now) {
        GrowthCalendar entry = growthCalendarRepository.findByFarmAndBlockForUpdate(farmId, blockCode)
                .stream()
                .findFirst()
                .orElse(null);
        if (entry == null) {
            entry = new GrowthCalendar();
            entry.setFarmId(farmId);
            entry.setBlockCode(blockCode);
        } else if (entry.getPlantingDate() != null && !entry.getPlantingDate().isAfter(plantingDate)) {
            // The calendar date is that of the oldest planting of the block:
            // the recorded date is the same or older, so the row stays as it is.
            return;
        }
        entry.setPlantingDate(plantingDate);
        entry.setDatePrecision(DAY_KNOWN_PRECISION);
        entry.setSource(USER_ENTRY_SOURCE);
        entry.setLastUpdated(now);
        growthCalendarRepository.save(entry);
    }
}

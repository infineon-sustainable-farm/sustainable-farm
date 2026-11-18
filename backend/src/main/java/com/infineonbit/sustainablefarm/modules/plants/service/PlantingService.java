package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.farm.api.BlockDto;
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
    private final PlantsBlocks plantsBlocks;

    /**
     * Block code as stored: trimmed and upper-cased, so {@code " a "} becomes
     * {@code "A"}. Prefixes and inner spaces were already refused by the
     * request validation.
     *
     * @param blockCode the block code as received
     * @return the block code as stored
     */
    static String normalizeBlockCode(String blockCode) {
        return blockCode.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * New variety row for a (block, name) pair planted for the first time.
     *
     * <p>{@code treeCount} is the number declared when the row is created, that
     * is the trees of this planting. The spacings and the vigor are those of the
     * planting, when given; density, yields and plant origin are not entered
     * with a planting: they stay NULL rather than being guessed.
     */
    private static Variety newVariety(BlockDto block, String name, Integer treeCount, Instant now) {
        Variety variety = new Variety();
        variety.assignBlock(block.id(), PlantsBlocks.label(block));
        variety.setName(name);
        variety.setTreeCount(treeCount);
        variety.setSource(USER_ENTRY_SOURCE);
        variety.setLastUpdated(now);
        return variety;
    }

    /**
     * Writes the spacings and the vigor of a planting on its variety row. A
     * value left out changes nothing, so an existing row keeps its own; a value
     * given replaces it. The vigor is trimmed and upper-cased ({@code " good "}
     * becomes {@code "GOOD"}).
     *
     * @param variety the variety row of the planting
     * @param request the planting, already validated: both spacings or none
     * @return {@code true} if a value was written
     */
    private static boolean applySpacingAndVigor(Variety variety, PlantingRequest request) {
        boolean written = false;
        if (request.rowSpacingM() != null) {
            variety.setRowSpacingM(request.rowSpacingM().doubleValue());
            variety.setTreeSpacingM(request.treeSpacingM().doubleValue());
            written = true;
        }
        if (request.vigor() != null) {
            variety.setVigor(request.vigor().trim().toUpperCase(Locale.ROOT));
            written = true;
        }
        return written;
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
     *     <li>Finds the block of the code in the farm referential, created when
     *         no block has it ({@link PlantsBlocks#resolve}), then the variety row
     *         of the (block, name) pair; the name ignores case. An existing row is
     *         reused and locked until the commit; a missing one is created. The spacings and the vigor given are written on the
     *         row, and replace those of an existing row.</li>
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
        BlockDto block = plantsBlocks.resolve(request.farmId(), normalizeBlockCode(request.blockCode()));
        String varietyName = request.varietyName().trim();

        Variety variety = varietyRepository.findByBlockAndNameForUpdate(block.id(), varietyName)
                .stream()
                .findFirst()
                .orElse(null);
        if (variety == null) {
            Variety created = newVariety(block, varietyName, request.treeCount(), now);
            applySpacingAndVigor(created, request);
            variety = varietyRepository.save(created);
        } else if (populationEventRepository.existsByVarietyIdAndEventType(variety.getId(), PopulationEventType.PLANTING)) {
            throw new ConflictException("A planting of " + variety.getName()
                    + " is already recorded on block " + variety.getBlockCode());
        } else if (applySpacingAndVigor(variety, request)) {
            variety.setLastUpdated(now);
            variety = varietyRepository.save(variety);
        }

        recordPlantingDate(block, request.plantingDate(), now);

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
     * @param block        block of the planting
     * @param plantingDate date of the planting being recorded
     * @param now          write time
     */
    private void recordPlantingDate(BlockDto block, LocalDate plantingDate, Instant now) {
        GrowthCalendar entry = growthCalendarRepository.findByBlockForUpdate(block.id())
                .stream()
                .findFirst()
                .orElse(null);
        if (entry == null) {
            entry = new GrowthCalendar();
            entry.assignBlock(block.id(), PlantsBlocks.label(block));
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

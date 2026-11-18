package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthCalendarResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.GrowthCalendarNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class GrowthCalendarService {

    private final GrowthCalendarRepository growthCalendarRepository;
    private final VarietyRepository varietyRepository;
    private final PlantsBlocks plantsBlocks;

    /**
     * Names of the varieties planted on the same block as the entry.
     *
     * <p>The link between the plants tables is made on the block id (column
     * {@code block_id}, a {@code farm.block} row): the block gives the farm, so a
     * block of one farm never borrows the varieties of another farm.
     *
     * <p>Every matching variety is returned — none is picked when there are
     * several. Names are de-duplicated and sorted so the output is stable.
     *
     * @param entry     the growth calendar entry
     * @param varieties candidate varieties, already loaded
     * @return the variety names, empty if the block has none
     */
    private static List<String> varietyNamesFor(GrowthCalendar entry, List<Variety> varieties) {
        return varieties.stream()
                .filter(variety -> Objects.equals(variety.getBlockId(), entry.getBlockId()))
                .map(Variety::getName)
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Maps an entity to its API representation, computing age and phase.
     *
     * <p>No stored value is derived or defaulted: a NULL column stays a
     * {@code null} component. Age and phase are computed, never read from storage.
     *
     * @param entry     the entity to map
     * @param varieties candidate varieties for the variety lookup
     * @param today     the reference date for the age
     * @return the API representation of that entry
     */
    private static GrowthCalendarResponse toResponse(GrowthCalendar entry, List<Variety> varieties, LocalDate today) {
        Period age = GrowthPhaseCalculator.computeAge(entry.getPlantingDate(), today);
        return new GrowthCalendarResponse(
                entry.getId(),
                entry.getFarmId(),
                entry.getBlockCode(),
                varietyNamesFor(entry, varieties),
                entry.getPlantingDate(),
                entry.getDatePrecision(),
                age == null ? null : age.getYears(),
                age == null ? null : age.getMonths(),
                GrowthPhaseCalculator.computePhase(age),
                GrowthPhaseCalculator.computePhaseYearsBand(age),
                entry.getCurrentStage(),
                entry.getPhaseYears(),
                entry.getLocalRainfallMm(),
                entry.getSource(),
                entry.getLastUpdated());
    }

    /**
     * Retrieves the growth calendar entries matching the optional filters.
     *
     * <p>Both filters are independent and optional. A filter that matches no row
     * is a normal outcome and returns an empty list; it is never an error.
     * Age and phase are computed against today's date.
     *
     * @param farmId    farm identifier, or {@code null} for every farm
     * @param blockCode block code (for example {@code "A"}), or {@code null}
     *                  or blank for every block
     * @return the matching entries ordered by block code, planting date and id, possibly empty
     */
    public List<GrowthCalendarResponse> getAllGrowthCalendarEntries(Long farmId, String blockCode) {
        return getAllGrowthCalendarEntries(farmId, blockCode, LocalDate.now());
    }

    /**
     * Same as {@link #getAllGrowthCalendarEntries(Long, String)}, against an
     * explicit reference date so the age computation can be tested.
     */
    List<GrowthCalendarResponse> getAllGrowthCalendarEntries(Long farmId, String blockCode, LocalDate today) {
        PlantsBlocks.BlockFilter blocks = plantsBlocks.filter(farmId, blockCode);
        List<GrowthCalendar> entries = PlantsBlocks.sortedByBlockCode(
                growthCalendarRepository.findByOptionalFilters(blocks.all(), blocks.ids()), GrowthCalendar::getBlockCode);
        if (entries.isEmpty()) {
            return List.of();
        }
        List<Variety> varieties = varietyRepository.findByOptionalFilters(blocks.all(), blocks.ids());
        return entries.stream().map(entry -> toResponse(entry, varieties, today)).toList();
    }

    /**
     * Retrieves a single growth calendar entry by its identifier.
     *
     * @param id the entry identifier
     * @return the representation of that entry, with age and phase computed against today
     * @throws GrowthCalendarNotFoundException if no entry exists with this ID
     */
    public GrowthCalendarResponse getGrowthCalendarEntryById(Long id) {
        return getGrowthCalendarEntryById(id, LocalDate.now());
    }

    /**
     * Same as {@link #getGrowthCalendarEntryById(Long)}, against an explicit
     * reference date so the age computation can be tested.
     */
    GrowthCalendarResponse getGrowthCalendarEntryById(Long id, LocalDate today) {
        GrowthCalendar entry = growthCalendarRepository.findById(id)
                .orElseThrow(() -> new GrowthCalendarNotFoundException(id));
        List<Variety> varieties = varietyRepository.findByOptionalFilters(false, List.of(entry.getBlockId()));
        return toResponse(entry, varieties, today);
    }
}

package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthCalendarResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.CalendrierCroissance;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.exception.CalendrierCroissanceNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.CalendrierCroissanceRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class CalendrierCroissanceService {

    private final CalendrierCroissanceRepository calendrierCroissanceRepository;
    private final VarietyRepository varietyRepository;

    /**
     * Growth Calendar Read Service
     * <p>Blank filter normalization method
     * <ul>
     *      <li>A filter sent as an empty or whitespace-only string means "no filter".</li>
     *      <li>It is turned into {@code null} so the query ignores it.</li>
     * </ul>
     */
    private static String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Names of the varieties planted on the same farm and block as the entry.
     *
     * <p>{@code bloc_parcelle} is the link between the plants tables. The farm must
     * match too, NULL matching NULL, so a block "A" of one farm never borrows the
     * varieties of block "A" of another farm.
     *
     * <p>Every matching variety is returned — none is picked when there are
     * several. Names are de-duplicated and sorted so the output is stable.
     *
     * @param entry     the growth calendar entry
     * @param varieties candidate varieties, already loaded
     * @return the variety names, empty if the block has none
     */
    private static List<String> varietyNamesFor(CalendrierCroissance entry, List<Variety> varieties) {
        return varieties.stream()
                .filter(variety -> Objects.equals(variety.getBlockCode(), entry.getBlocParcelle()))
                .filter(variety -> Objects.equals(variety.getFarmId(), entry.getIdFerme()))
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
    private static GrowthCalendarResponse toResponse(CalendrierCroissance entry, List<Variety> varieties, LocalDate today) {
        Period age = GrowthPhaseCalculator.computeAge(entry.getDatePlantation(), today);
        return new GrowthCalendarResponse(
                entry.getId(),
                entry.getIdFerme(),
                entry.getBlocParcelle(),
                varietyNamesFor(entry, varieties),
                entry.getDatePlantation(),
                entry.getPrecisionDate(),
                age == null ? null : age.getYears(),
                age == null ? null : age.getMonths(),
                GrowthPhaseCalculator.computePhase(age),
                GrowthPhaseCalculator.computePhaseYearsBand(age),
                entry.getStadeActuel(),
                entry.getPhaseAnnees(),
                entry.getPluviometrieLocaleMm(),
                entry.getSource(),
                entry.getDateMaj());
    }

    /**
     * Retrieves the growth calendar entries matching the optional filters.
     *
     * <p>Both filters are independent and optional. A filter that matches no row
     * is a normal outcome and returns an empty list; it is never an error.
     * Age and phase are computed against today's date.
     *
     * @param idFerme      farm identifier, or {@code null} for every farm
     * @param blocParcelle raw block value as stored (for example {@code "A"}),
     *                     or {@code null} for every block
     * @return the matching entries, possibly empty
     */
    public List<GrowthCalendarResponse> obtainAllGrowthCalendarEntries(Integer idFerme, String blocParcelle) {
        return obtainAllGrowthCalendarEntries(idFerme, blocParcelle, LocalDate.now());
    }

    /**
     * Same as {@link #obtainAllGrowthCalendarEntries(Integer, String)}, against an
     * explicit reference date so the age computation can be tested.
     */
    List<GrowthCalendarResponse> obtainAllGrowthCalendarEntries(Integer idFerme, String blocParcelle, LocalDate today) {
        String normalizedBloc = normalizeFilter(blocParcelle);
        List<CalendrierCroissance> entries = calendrierCroissanceRepository.findByOptionalFilters(idFerme, normalizedBloc);
        if (entries.isEmpty()) {
            return List.of();
        }
        // Farm matching is done in memory, NULL-safe; only the block narrows the query.
        List<Variety> varieties = varietyRepository.findByOptionalFilters(null, normalizedBloc);
        return entries.stream().map(entry -> toResponse(entry, varieties, today)).toList();
    }

    /**
     * Retrieves a single growth calendar entry by its identifier.
     *
     * @param id the entry identifier
     * @return the representation of that entry, with age and phase computed against today
     * @throws CalendrierCroissanceNotFoundException if no entry exists with this ID
     */
    public GrowthCalendarResponse obtainGrowthCalendarEntryById(Long id) {
        return obtainGrowthCalendarEntryById(id, LocalDate.now());
    }

    /**
     * Same as {@link #obtainGrowthCalendarEntryById(Long)}, against an explicit
     * reference date so the age computation can be tested.
     */
    GrowthCalendarResponse obtainGrowthCalendarEntryById(Long id, LocalDate today) {
        CalendrierCroissance entry = calendrierCroissanceRepository.findById(id)
                .orElseThrow(() -> new CalendrierCroissanceNotFoundException(id));
        List<Variety> varieties = varietyRepository.findByOptionalFilters(null, entry.getBlocParcelle());
        return toResponse(entry, varieties, today);
    }
}

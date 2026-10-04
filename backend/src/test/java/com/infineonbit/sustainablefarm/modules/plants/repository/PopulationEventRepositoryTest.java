package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEvent;
import com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.PopulationEventRepository.TreeBalance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.EXTENSION;
import static com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.MORTALITY;
import static com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.PLANTING;
import static com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.REMOVAL;
import static com.infineonbit.sustainablefarm.modules.plants.entity.PopulationEventType.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class PopulationEventRepositoryTest {

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private PopulationEventRepository populationEventRepository;

    private Variety variety(String name) {
        Variety variety = new Variety();
        variety.setBlockCode("A");
        variety.setName(name);
        return varietyRepository.save(variety);
    }

    private Variety variety(Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return varietyRepository.save(variety);
    }

    private List<String> plantedNames(Integer farmId, String blockCode) {
        return populationEventRepository.findPlantingsByOptionalFilters(farmId, blockCode).stream()
                .map(planting -> planting.getVariety().getBlockCode() + " " + planting.getVariety().getName())
                .toList();
    }

    private void event(Variety variety, PopulationEventType type, int treeCount) {
        PopulationEvent event = new PopulationEvent();
        event.setVariety(variety);
        event.setEventType(type);
        event.setEventDate(LocalDate.of(2024, 1, 15));
        event.setTreeCount(treeCount);
        event.setSource("user_entry");
        populationEventRepository.save(event);
    }

    private Map<Long, Long> balances(Variety... varieties) {
        List<Long> ids = Arrays.stream(varieties).map(Variety::getId).toList();
        return populationEventRepository.findTreeBalances(ids).stream()
                .collect(Collectors.toMap(TreeBalance::getVarietyId, TreeBalance::getBalance));
    }

    @Test
    void findTreeBalances_shouldReturnNoLine_whenVarietyHasNoEvent() {
        // Arrange
        Variety keitt = variety("Keitt");
        // Act & Assert: no event means an unknown count, not zero
        assertTrue(balances(keitt).isEmpty());
    }

    @Test
    void findTreeBalances_shouldAddAndSubtractByEventType() {
        // Arrange
        Variety keitt = variety("Keitt");
        event(keitt, PLANTING, 100);
        event(keitt, EXTENSION, 20);
        event(keitt, REPLACEMENT, 5);
        event(keitt, MORTALITY, 7);
        event(keitt, REMOVAL, 3);
        // Act & Assert: 100 + 20 + 5 - 7 - 3
        assertEquals(Map.of(keitt.getId(), 115L), balances(keitt));
    }

    @Test
    void findTreeBalances_shouldGiveOneBalancePerVariety() {
        // Arrange
        Variety keitt = variety("Keitt");
        Variety kent = variety("Kent");
        Variety amelie = variety("Amelie");
        event(keitt, PLANTING, 150);
        event(kent, PLANTING, 40);
        event(kent, MORTALITY, 2);
        // Act & Assert: Amelie has no event, so no line
        assertEquals(Map.of(keitt.getId(), 150L, kent.getId(), 38L), balances(keitt, kent, amelie));
    }

    @Test
    void existsByVarietyIdAndEventType_shouldFindOnlyThePlantingOfThatVariety() {
        // Arrange
        Variety keitt = variety("Keitt");
        Variety kent = variety("Kent");
        event(keitt, PLANTING, 150);
        // Act & Assert
        assertTrue(populationEventRepository.existsByVarietyIdAndEventType(keitt.getId(), PLANTING));
        assertFalse(populationEventRepository.existsByVarietyIdAndEventType(kent.getId(), PLANTING));
        assertFalse(populationEventRepository.existsByVarietyIdAndEventType(keitt.getId(), MORTALITY));
    }

    @Test
    void findFirstByVarietyIdAndEventType_shouldReturnThePlantingOfThatVarietyOnly() {
        // Arrange: a later mortality must not be taken for the planting
        Variety keitt = variety("Keitt");
        Variety kent = variety("Kent");
        PopulationEvent planting = new PopulationEvent(
                null, keitt, PLANTING, LocalDate.of(2023, 9, 24), 150, "user_entry", null);
        populationEventRepository.save(planting);
        event(keitt, MORTALITY, 2);
        // Act & Assert
        assertEquals(LocalDate.of(2023, 9, 24), populationEventRepository
                .findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(keitt.getId(), PLANTING)
                .orElseThrow()
                .getEventDate());
        assertTrue(populationEventRepository
                .findFirstByVarietyIdAndEventTypeOrderByEventDateAsc(kent.getId(), PLANTING)
                .isEmpty());
    }

    @Test
    void findPlantingsByOptionalFilters_shouldSkipRowsWithoutPlanting_likeTheZalkaRow() {
        // Arrange: the Zalka row has no event; another row has a mortality only
        variety(null, "A", "Keitt");
        Variety kent = variety(null, "A", "Kent");
        event(kent, MORTALITY, 1);
        Variety keittB = variety(null, "B", "Keitt");
        event(keittB, PLANTING, 150);
        event(keittB, MORTALITY, 2);
        // Act & Assert: only the planting, not the mortality of the same row
        assertEquals(List.of("B Keitt"), plantedNames(null, null));
    }

    @Test
    void findPlantingsByOptionalFilters_shouldOrderByBlockThenName_andLoadTheVariety() {
        // Arrange: saved out of order
        event(variety(null, "B", "Keitt"), PLANTING, 150);
        event(variety(null, "A", "Palmer"), PLANTING, 10);
        event(variety(null, "A", "Kent"), PLANTING, 40);
        // Act
        List<PopulationEvent> plantings = populationEventRepository.findPlantingsByOptionalFilters(null, null);
        // Assert
        assertEquals(List.of("A Kent", "A Palmer", "B Keitt"), plantedNames(null, null));
        assertEquals(PLANTING, plantings.get(0).getEventType());
        assertEquals(40, plantings.get(0).getTreeCount());
    }

    @Test
    void findPlantingsByOptionalFilters_shouldFilterOnFarmAndBlock() {
        // Arrange
        event(variety(null, "B", "Keitt"), PLANTING, 150);
        event(variety(1, "B", "Keitt"), PLANTING, 80);
        event(variety(1, "A", "Kent"), PLANTING, 40);
        // Act & Assert: no farm filter means every farm; farm 1 leaves the row without a farm out
        assertEquals(List.of("A Kent", "B Keitt", "B Keitt"), plantedNames(null, null));
        assertEquals(List.of("A Kent", "B Keitt"), plantedNames(1, null));
        assertEquals(List.of("B Keitt"), plantedNames(1, "B"));
        assertEquals(List.of("B Keitt", "B Keitt"), plantedNames(null, "B"));
        assertTrue(plantedNames(2, null).isEmpty());
    }
}

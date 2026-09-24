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
}

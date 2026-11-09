package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityObservation;
import com.infineonbit.sustainablefarm.modules.plants.entity.MaturityStatus;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class MaturityObservationRepositoryTest {

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private MaturityObservationRepository maturityObservationRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Variety variety(Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return varietyRepository.save(variety);
    }

    private MaturityObservation observation(Variety variety, LocalDate observedOn) {
        return maturityObservationRepository.save(new MaturityObservation(null, variety, observedOn,
                MaturityStatus.RIPE, MaturityMethod.VISUAL, null, null, "user_entry", null));
    }

    private List<Long> find(Integer farmId, String blockCode, LocalDate from, LocalDate to) {
        return maturityObservationRepository.findByOptionalFilters(farmId, blockCode, from, to).stream()
                .map(MaturityObservation::getId)
                .toList();
    }

    @Test
    void findByOptionalFilters_shouldReturnEveryObservationByDateThenId_whenNoFilterIsGiven() {
        // Arrange: two observations on the same day, saved out of date order
        Variety keitt = variety(null, "B", "Keitt");
        MaturityObservation late = observation(keitt, LocalDate.of(2026, 10, 20));
        MaturityObservation first = observation(keitt, LocalDate.of(2026, 10, 12));
        MaturityObservation second = observation(keitt, LocalDate.of(2026, 10, 12));
        // Act & Assert
        assertEquals(List.of(first.getId(), second.getId(), late.getId()), find(null, null, null, null));
    }

    @Test
    void findByOptionalFilters_shouldKeepFarmNullAndFarm1Apart() {
        // Arrange: the same block and variety on no farm and on farm 1
        MaturityObservation noFarm = observation(variety(null, "B", "Keitt"), LocalDate.of(2026, 10, 12));
        MaturityObservation farm1 = observation(variety(1, "B", "Keitt"), LocalDate.of(2026, 10, 12));
        // Act & Assert: farm 1 excludes the row without a farm; no farm filter means every farm
        assertEquals(List.of(farm1.getId()), find(1, null, null, null));
        assertEquals(List.of(noFarm.getId(), farm1.getId()), find(null, null, null, null));
        assertTrue(find(2, null, null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldFilterOnTheBlockOfTheVariety() {
        // Arrange
        MaturityObservation blockB = observation(variety(null, "B", "Keitt"), LocalDate.of(2026, 10, 12));
        observation(variety(null, "A", "Kent"), LocalDate.of(2026, 10, 2));
        // Act & Assert
        assertEquals(List.of(blockB.getId()), find(null, "B", null, null));
        assertTrue(find(null, "C", null, null).isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldIncludeBothBounds_andApplyASingleOne() {
        // Arrange
        Variety keitt = variety(null, "B", "Keitt");
        MaturityObservation september = observation(keitt, LocalDate.of(2026, 9, 30));
        MaturityObservation first = observation(keitt, LocalDate.of(2026, 10, 1));
        MaturityObservation last = observation(keitt, LocalDate.of(2026, 10, 31));
        MaturityObservation november = observation(keitt, LocalDate.of(2026, 11, 1));
        // Act & Assert
        assertEquals(List.of(first.getId(), last.getId()),
                find(null, null, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        assertEquals(List.of(first.getId(), last.getId(), november.getId()),
                find(null, null, LocalDate.of(2026, 10, 1), null));
        assertEquals(List.of(september.getId()), find(null, null, null, LocalDate.of(2026, 9, 30)));
    }

    @Test
    void findByOptionalFilters_shouldReturnNothing_whenFromIsAfterTo() {
        // Arrange
        observation(variety(null, "B", "Keitt"), LocalDate.of(2026, 10, 12));
        // Act & Assert: an empty list, not an error
        assertTrue(find(null, null, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 1)).isEmpty());
    }

    @Test
    void save_shouldStoreEveryField_asGiven() {
        // Arrange: a Brix reading, and a visual observation without value
        Variety keitt = variety(null, "B", "Keitt");
        Instant now = Instant.parse("2026-10-25T10:00:00Z");
        Long brix = maturityObservationRepository.save(new MaturityObservation(null, keitt,
                LocalDate.of(2026, 10, 12), MaturityStatus.READY_FOR_HARVEST, MaturityMethod.BRIX,
                new BigDecimal("14.2"), "Awa", "user_entry", now)).getId();
        Long visual = observation(keitt, LocalDate.of(2026, 10, 13)).getId();
        entityManager.flush();
        entityManager.clear();
        // Act
        MaturityObservation storedBrix = maturityObservationRepository.findById(brix).orElseThrow();
        MaturityObservation storedVisual = maturityObservationRepository.findById(visual).orElseThrow();
        // Assert: the value with its one decimal, the codes, the observer and the source
        assertEquals(keitt.getId(), storedBrix.getVariety().getId());
        assertEquals(LocalDate.of(2026, 10, 12), storedBrix.getObservedOn());
        assertEquals(MaturityStatus.READY_FOR_HARVEST, storedBrix.getMaturityStatus());
        assertEquals(MaturityMethod.BRIX, storedBrix.getMethod());
        assertEquals(new BigDecimal("14.2"), storedBrix.getMeasuredValue());
        assertEquals("Awa", storedBrix.getObserver());
        assertEquals("user_entry", storedBrix.getSource());
        assertEquals(now, storedBrix.getLastUpdated());
        assertEquals(MaturityMethod.VISUAL, storedVisual.getMethod());
        assertNull(storedVisual.getMeasuredValue());
        assertNull(storedVisual.getObserver());
    }
}

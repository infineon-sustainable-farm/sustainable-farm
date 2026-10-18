package com.infineonbit.sustainablefarm.modules.plants.repository;

import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
public class VarietyRepositoryTest {

    @Autowired
    private VarietyRepository varietyRepository;

    private static Variety variety(Integer farmId, String blockCode, String name) {
        Variety variety = new Variety();
        variety.setFarmId(farmId);
        variety.setBlockCode(blockCode);
        variety.setName(name);
        return variety;
    }

    private Variety save(Integer farmId, String blockCode, String name) {
        return varietyRepository.save(variety(farmId, blockCode, name));
    }

    private static List<Long> ids(List<Variety> varieties) {
        return varieties.stream().map(Variety::getId).toList();
    }

    @Test
    void findByFarmBlockAndName_shouldMatchOnlyRowsWithoutFarm_whenFarmIsNull() {
        // Arrange
        Variety noFarm = save(null, "A", "Keitt");
        save(1, "A", "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByFarmBlockAndName(null, "A", "Keitt");
        // Assert
        assertEquals(List.of(noFarm.getId()), ids(found));
    }

    @Test
    void findByFarmBlockAndName_shouldMatchOnlyThatFarm_whenFarmIsGiven() {
        // Arrange
        save(null, "A", "Keitt");
        Variety farm1 = save(1, "A", "Keitt");
        save(2, "A", "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByFarmBlockAndName(1, "A", "Keitt");
        // Assert
        assertEquals(List.of(farm1.getId()), ids(found));
    }

    @Test
    void findByFarmBlockAndName_shouldIgnoreNameCase() {
        // Arrange
        Variety keitt = save(null, "A", "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByFarmBlockAndName(null, "A", "kEITT");
        // Assert
        assertEquals(List.of(keitt.getId()), ids(found));
    }

    @Test
    void findByFarmBlockAndName_shouldReturnNothing_whenBlockDiffers() {
        // Arrange
        save(null, "A", "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByFarmBlockAndName(null, "B", "Keitt");
        // Assert
        assertTrue(found.isEmpty());
    }

    @Test
    void findByOptionalFilters_shouldStillReadNullFarmAsEveryFarm() {
        // Arrange: the list filter keeps its meaning, unlike the planting lookup
        save(null, "A", "Keitt");
        save(1, "A", "Keitt");
        // Act
        List<Variety> found = varietyRepository.findByOptionalFilters(null, "A");
        // Assert
        assertEquals(2, found.size());
    }

    @Test
    void findByFarmBlockAndNameForUpdate_shouldMatchTheSameRowsAsTheSharedLookup() {
        // Arrange
        Variety noFarm = save(null, "A", "Keitt");
        Variety farm1 = save(1, "A", "Keitt");
        save(null, "B", "Keitt");
        // Act: the locked lookup runs inside the transaction of the test
        List<Variety> foundWithoutFarm = varietyRepository.findByFarmBlockAndNameForUpdate(null, "A", "kEITT");
        List<Variety> foundForFarm1 = varietyRepository.findByFarmBlockAndNameForUpdate(1, "A", "Keitt");
        // Assert
        assertEquals(List.of(noFarm.getId()), ids(foundWithoutFarm));
        assertEquals(List.of(farm1.getId()), ids(foundForFarm1));
        assertEquals(ids(varietyRepository.findByFarmBlockAndName(null, "A", "kEITT")), ids(foundWithoutFarm));
    }

    @Test
    void save_shouldRefuseASecondRowOfTheSameTriple_whenFarmIsNull() {
        // Arrange
        varietyRepository.saveAndFlush(variety(null, "A", "Keitt"));
        // Act & Assert: a plain UNIQUE on the columns would let two NULL farms through
        assertThrows(DataIntegrityViolationException.class,
                () -> varietyRepository.saveAndFlush(variety(null, "A", "Keitt")));
    }

    @Test
    void save_shouldRefuseASecondRowOfTheSameTriple_whenOnlyTheCaseOfTheNameDiffers() {
        // Arrange
        varietyRepository.saveAndFlush(variety(1, "A", "Keitt"));
        // Act & Assert: the planting lookup already reads "keitt" as "Keitt"
        assertThrows(DataIntegrityViolationException.class,
                () -> varietyRepository.saveAndFlush(variety(1, "A", "keitt")));
    }
}

package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The catalogue loader runs against a real database. It is built by hand, not imported as
 * a bean: a bean would already run at context startup, before the test.
 */
@DataJpaTest
@ActiveProfiles("test")
public class VarietyCatalogLoaderTest {

    private static final Instant FIRST_START = Instant.parse("2026-09-24T10:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2026-10-01T08:00:00Z");

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    private VarietyCatalogLoader loader;

    @BeforeEach
    void setUp() {
        loader = new VarietyCatalogLoader(varietyReferenceRepository);
    }

    private Map<String, VarietyReference> varietiesByName() {
        return varietyReferenceRepository.findAll().stream()
                .collect(Collectors.toMap(VarietyReference::getVarietyName, reference -> reference));
    }

    private static void assertVariety(VarietyReference reference, double yieldPerTreeKg, String yieldSource,
                                      int harvestStartMonth, int harvestEndMonth, String seasonSource) {
        assertEquals(yieldPerTreeKg, reference.getYieldPerTreeKg());
        assertEquals(yieldSource, reference.getYieldSource());
        assertEquals(harvestStartMonth, reference.getHarvestStartMonth());
        assertEquals(harvestEndMonth, reference.getHarvestEndMonth());
        assertEquals(seasonSource, reference.getSeasonSource());
        assertEquals(FIRST_START, reference.getLastUpdated());
    }

    @Test
    void load_shouldInsertTheCatalogue_whenTheTableIsEmpty() {
        // Act
        loader.load(FIRST_START);
        // Assert: the five varieties of the catalogue, with their keys, values and sources
        Map<String, VarietyReference> varieties = varietiesByName();
        assertEquals(5, varieties.size());
        assertVariety(varieties.get("Keitt"), 220.0, "Zalka_2025", 5, 7, "varietal_guide_west_africa");
        assertVariety(varieties.get("Kent"), 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina");
        assertVariety(varieties.get("Amelie"), 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina");
        assertVariety(varieties.get("Tommy Atkins"), 180.0, "assumption_to_validate", 4, 6, "assumption_to_validate");
        assertVariety(varieties.get("Other"), 160.0, "assumption_to_validate", 4, 6, "assumption_to_validate");
        assertEquals(Map.of("Keitt", "KEITT", "Kent", "KENT", "Amelie", "AMELIE", "Tommy Atkins", "TOMMY",
                        "Other", "OTHER"),
                varieties.values().stream().collect(Collectors.toMap(VarietyReference::getVarietyName,
                        VarietyReference::getVarietyKey)));
    }

    @Test
    void load_shouldNotDuplicateRows_whenRunTwice() {
        // Act
        loader.load(FIRST_START);
        loader.load(SECOND_START);
        // Assert: same rows, still dated from the first start
        assertEquals(5, varietyReferenceRepository.count());
        varietyReferenceRepository.findAll()
                .forEach(reference -> assertEquals(FIRST_START, reference.getLastUpdated()));
    }

    @Test
    void load_shouldKeepAValueChangedInTheDatabase() {
        // Arrange: a mentor lowers the Keitt yield
        loader.load(FIRST_START);
        VarietyReference keitt = varietiesByName().get("Keitt");
        keitt.setYieldPerTreeKg(80.0);
        keitt.setYieldSource("orchard_literature");
        varietyReferenceRepository.save(keitt);
        // Act
        loader.load(SECOND_START);
        // Assert: the changed values stay, nothing is added
        assertEquals(80.0, varietiesByName().get("Keitt").getYieldPerTreeKg());
        assertEquals("orchard_literature", varietiesByName().get("Keitt").getYieldSource());
        assertEquals(5, varietyReferenceRepository.count());
    }

    @Test
    void load_shouldNotReinsertAVariety_whenItsNameWasRewrittenWithAccents() {
        // Arrange: the Amelie row renamed "Amélie" by hand
        loader.load(FIRST_START);
        VarietyReference amelie = varietiesByName().get("Amelie");
        amelie.setVarietyName("Amélie");
        varietyReferenceRepository.save(amelie);
        // Act
        loader.load(SECOND_START);
        // Assert: no second "Amelie" row
        assertEquals(List.of("Amélie", "Keitt", "Kent", "Other", "Tommy Atkins"), varietiesByName().keySet().stream().sorted().toList());
    }

    @Test
    void load_shouldNotInsertAVariety_whoseKeyIsTaken() {
        // Arrange: "Tommy" entered by the user before the first start, key TOMMY
        varietyReferenceRepository.save(new VarietyReference(null, "Tommy", 170.0, "user_entry", 4, 6,
                "user_entry", FIRST_START));
        // Act
        loader.load(SECOND_START);
        // Assert: no "Tommy Atkins" row under the same key
        assertEquals(List.of("Amelie", "Keitt", "Kent", "Other", "Tommy"),
                varietiesByName().keySet().stream().sorted().toList());
    }

    @Test
    void load_shouldInsertOnlyTheMissingVarieties() {
        // Arrange: a variety deleted by hand
        loader.load(FIRST_START);
        varietyReferenceRepository.delete(varietiesByName().get("Kent"));
        // Act
        loader.load(SECOND_START);
        // Assert: it is back, dated from the second start; the others are untouched
        assertEquals(SECOND_START, varietiesByName().get("Kent").getLastUpdated());
        assertEquals(FIRST_START, varietiesByName().get("Keitt").getLastUpdated());
        assertEquals(5, varietyReferenceRepository.count());
    }
}

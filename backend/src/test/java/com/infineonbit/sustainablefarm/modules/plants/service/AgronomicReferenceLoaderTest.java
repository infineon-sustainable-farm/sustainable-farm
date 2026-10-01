package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The loader runs against a real database. It is built by hand, not imported as
 * a bean: a bean would already run at context startup, before the test.
 */
@DataJpaTest
@ActiveProfiles("test")
public class AgronomicReferenceLoaderTest {

    private static final Instant FIRST_START = Instant.parse("2026-09-24T10:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2026-10-01T08:00:00Z");

    @Autowired
    private VarietyReferenceRepository varietyReferenceRepository;

    @Autowired
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    private AgronomicReferenceLoader loader;

    @BeforeEach
    void setUp() {
        loader = new AgronomicReferenceLoader(varietyReferenceRepository, growthPhaseYieldShareRepository);
    }

    private Map<String, VarietyReference> varietiesByName() {
        return varietyReferenceRepository.findAll().stream()
                .collect(Collectors.toMap(VarietyReference::getVarietyName, reference -> reference));
    }

    private Map<String, GrowthPhaseYieldShare> sharesByPhase() {
        return growthPhaseYieldShareRepository.findAll().stream()
                .collect(Collectors.toMap(GrowthPhaseYieldShare::getGrowthPhase, share -> share));
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

    private static void assertShare(GrowthPhaseYieldShare share, double yieldShare, String source) {
        assertEquals(yieldShare, share.getYieldShare());
        assertEquals(source, share.getSource());
        assertEquals(FIRST_START, share.getLastUpdated());
    }

    @Test
    void load_shouldInsertEveryDefault_whenTablesAreEmpty() {
        // Act
        loader.load(FIRST_START);
        // Assert: the three varieties, with their values and sources
        Map<String, VarietyReference> varieties = varietiesByName();
        assertEquals(3, varieties.size());
        assertVariety(varieties.get("Keitt"), 220.0, "Zalka_2025", 5, 7, "varietal_guide_west_africa");
        assertVariety(varieties.get("Kent"), 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina");
        assertVariety(varieties.get("Amelie"), 160.0, "Zalka_2025", 2, 4, "FAO_mango_burkina");
        // Assert: the three phases
        Map<String, GrowthPhaseYieldShare> shares = sharesByPhase();
        assertEquals(3, shares.size());
        assertShare(shares.get("establishment"), 0.0, "orchard_literature");
        assertShare(shares.get("gradual production"), 0.5, "assumption_to_validate");
        assertShare(shares.get("full production"), 1.0, "by_definition");
    }

    @Test
    void load_shouldCoverEveryPhaseTheCalculatorReturns() {
        // Act
        loader.load(FIRST_START);
        // Assert: the forecast finds a share for the phase of any age
        Map<String, GrowthPhaseYieldShare> shares = sharesByPhase();
        Stream.of(Period.ofYears(0), Period.ofYears(3), Period.ofYears(6))
                .map(GrowthPhaseCalculator::computePhase)
                .forEach(phase -> assertTrue(shares.containsKey(phase), phase));
    }

    @Test
    void load_shouldNotDuplicateRows_whenRunTwice() {
        // Act
        loader.load(FIRST_START);
        loader.load(SECOND_START);
        // Assert: same rows, still dated from the first start
        assertEquals(3, varietyReferenceRepository.count());
        assertEquals(3, growthPhaseYieldShareRepository.count());
        varietyReferenceRepository.findAll()
                .forEach(reference -> assertEquals(FIRST_START, reference.getLastUpdated()));
        growthPhaseYieldShareRepository.findAll()
                .forEach(share -> assertEquals(FIRST_START, share.getLastUpdated()));
    }

    @Test
    void load_shouldKeepAValueChangedInTheDatabase() {
        // Arrange: a mentor lowers the Keitt yield and the gradual production share
        loader.load(FIRST_START);
        VarietyReference keitt = varietiesByName().get("Keitt");
        keitt.setYieldPerTreeKg(80.0);
        keitt.setYieldSource("orchard_literature");
        varietyReferenceRepository.save(keitt);
        GrowthPhaseYieldShare gradual = sharesByPhase().get("gradual production");
        gradual.setYieldShare(0.3);
        growthPhaseYieldShareRepository.save(gradual);
        // Act
        loader.load(SECOND_START);
        // Assert: the changed values stay, nothing is added
        assertEquals(80.0, varietiesByName().get("Keitt").getYieldPerTreeKg());
        assertEquals("orchard_literature", varietiesByName().get("Keitt").getYieldSource());
        assertEquals(0.3, sharesByPhase().get("gradual production").getYieldShare());
        assertEquals(3, varietyReferenceRepository.count());
        assertEquals(3, growthPhaseYieldShareRepository.count());
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
        assertEquals(List.of("Amélie", "Keitt", "Kent"), varietiesByName().keySet().stream().sorted().toList());
    }

    @Test
    void load_shouldInsertOnlyTheMissingRows() {
        // Arrange: a phase and a variety deleted by hand
        loader.load(FIRST_START);
        varietyReferenceRepository.delete(varietiesByName().get("Kent"));
        growthPhaseYieldShareRepository.delete(sharesByPhase().get("full production"));
        // Act
        loader.load(SECOND_START);
        // Assert: they are back, dated from the second start; the others are untouched
        assertEquals(SECOND_START, varietiesByName().get("Kent").getLastUpdated());
        assertEquals(SECOND_START, sharesByPhase().get("full production").getLastUpdated());
        assertEquals(FIRST_START, varietiesByName().get("Keitt").getLastUpdated());
        assertEquals(FIRST_START, sharesByPhase().get("establishment").getLastUpdated());
        assertEquals(3, varietyReferenceRepository.count());
        assertEquals(3, growthPhaseYieldShareRepository.count());
    }
}

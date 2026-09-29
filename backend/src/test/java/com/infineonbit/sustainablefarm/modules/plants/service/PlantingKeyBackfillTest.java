package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthCalendar;
import com.infineonbit.sustainablefarm.modules.plants.entity.Variety;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthCalendarRepository;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The filling runs against a real database, each row in its own transaction
 * as at startup, so the test itself runs outside any transaction and removes
 * its rows afterwards. It is built by hand, not imported as a bean: a bean
 * would already run at context startup, before the test.
 *
 * <p>The rows written before the keys existed are inserted with SQL, since
 * the entities always write their key.
 */
@DataJpaTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlantingKeyBackfillTest {

    private static final Instant WRITTEN = Instant.parse("2026-09-01T08:00:00Z");

    @Autowired
    private VarietyRepository varietyRepository;

    @Autowired
    private GrowthCalendarRepository growthCalendarRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    private PlantingKeyBackfill backfill;

    @BeforeEach
    void setUp() {
        backfill = new PlantingKeyBackfill(varietyRepository, growthCalendarRepository, transactionManager);
    }

    @AfterEach
    void removeTheRowsOfTheTest() {
        jdbc.update("delete from varietes where bloc_parcelle like 'BF%'");
        jdbc.update("delete from calendrier_croissance where bloc_parcelle like 'BF%'");
    }

    /** A variety row as written before the key existed. */
    private Long insertVarietyWithoutKey(Integer farmId, String blockCode, String name) {
        jdbc.update("insert into varietes (id_ferme, bloc_parcelle, nom, source) values (?, ?, ?, 'Zalka_2025')",
                farmId, blockCode, name);
        return jdbc.queryForObject("select max(id) from varietes", Long.class);
    }

    /** A calendar row as written before the key existed. */
    private Long insertCalendarWithoutKey(Integer farmId, String blockCode) {
        jdbc.update("insert into calendrier_croissance (id_ferme, bloc_parcelle, source) values (?, ?, 'Zalka_2025')",
                farmId, blockCode);
        return jdbc.queryForObject("select max(id) from calendrier_croissance", Long.class);
    }

    private String varietyKey(Long id) {
        return varietyRepository.findById(id).orElseThrow().getVarietyKey();
    }

    private String blockKey(Long id) {
        return growthCalendarRepository.findById(id).orElseThrow().getBlockKey();
    }

    @Test
    void fill_shouldWriteTheKeyOfEveryRowWithoutOne() {
        // Arrange
        Long keitt = insertVarietyWithoutKey(null, "BF1", "Keitt");
        Long kent = insertVarietyWithoutKey(1, "BF1", "Kent");
        Long calendar = insertCalendarWithoutKey(null, "BF1");
        // Act
        PlantingKeyBackfill.Result result = backfill.fill();
        // Assert
        assertEquals("0|BF1|keitt", varietyKey(keitt));
        assertEquals("1|BF1|kent", varietyKey(kent));
        assertEquals("0|BF1", blockKey(calendar));
        assertEquals(new PlantingKeyBackfill.Result(2, List.of(), 1, List.of()), result);
    }

    @Test
    void fill_shouldLeaveTheLaterVarietyDuplicatesWithoutKey_andDeleteNothing() {
        // Arrange: three rows of one triple, the name in two cases
        Long oldest = insertVarietyWithoutKey(null, "BF2", "Keitt");
        Long sameName = insertVarietyWithoutKey(null, "BF2", "Keitt");
        Long otherCase = insertVarietyWithoutKey(null, "BF2", "keitt");
        // Act
        PlantingKeyBackfill.Result result = backfill.fill();
        // Assert: the oldest row keeps the key, the other two are reported and kept
        assertEquals("0|BF2|keitt", varietyKey(oldest));
        assertNull(varietyKey(sameName));
        assertNull(varietyKey(otherCase));
        assertEquals(List.of(sameName, otherCase), result.varietyDuplicates());
        assertEquals(3, varietyRepository.findByOptionalFilters(null, "BF2").size());
    }

    @Test
    void fill_shouldLeaveTheLaterCalendarDuplicateWithoutKey_andDeleteNothing() {
        // Arrange
        Long oldest = insertCalendarWithoutKey(null, "BF3");
        Long duplicate = insertCalendarWithoutKey(null, "BF3");
        Long otherFarm = insertCalendarWithoutKey(1, "BF3");
        // Act
        PlantingKeyBackfill.Result result = backfill.fill();
        // Assert: farm NULL and farm 1 are two blocks; only the true duplicate is left out
        assertEquals("0|BF3", blockKey(oldest));
        assertNull(blockKey(duplicate));
        assertEquals("1|BF3", blockKey(otherFarm));
        assertEquals(List.of(duplicate), result.calendarDuplicates());
        assertEquals(3, growthCalendarRepository.findByOptionalFilters(null, "BF3").size());
    }

    @Test
    void fill_shouldChangeNoLastUpdate_andLeaveRowsWithAKeyAlone() {
        // Arrange: a row written through the entity, which has its key, and an older one without
        Variety current = new Variety();
        current.setBlockCode("BF4");
        current.setName("Kent");
        current.setLastUpdated(WRITTEN);
        Long withKey = varietyRepository.save(current).getId();
        Long withoutKey = varietyRepository.save(copyOf(current, "Keitt")).getId();
        jdbc.update("update varietes set variety_key = null where id = ?", withoutKey);
        Instant lastUpdateBefore = varietyRepository.findById(withoutKey).orElseThrow().getLastUpdated();
        // Act
        PlantingKeyBackfill.Result result = backfill.fill();
        // Assert
        assertEquals(1, result.varietiesKeyed());
        assertEquals("0|BF4|keitt", varietyKey(withoutKey));
        assertEquals(lastUpdateBefore, varietyRepository.findById(withoutKey).orElseThrow().getLastUpdated());
        assertEquals("0|BF4|kent", varietyKey(withKey));
        assertEquals(WRITTEN, varietyRepository.findById(withKey).orElseThrow().getLastUpdated());
    }

    private static Variety copyOf(Variety variety, String name) {
        Variety copy = new Variety();
        copy.setBlockCode(variety.getBlockCode());
        copy.setName(name);
        copy.setLastUpdated(variety.getLastUpdated());
        return copy;
    }

    @Test
    void run_shouldNeverStopTheStartup_whenTheFillingFails() {
        // Arrange: the database cannot be read
        VarietyRepository unavailable = mock(VarietyRepository.class);
        when(unavailable.findByVarietyKeyIsNullOrderByIdAsc()).thenThrow(new IllegalStateException("database down"));
        PlantingKeyBackfill failing = new PlantingKeyBackfill(unavailable, growthCalendarRepository,
                transactionManager);
        // Act & Assert: the failure is logged, not thrown
        assertDoesNotThrow(() -> failing.run());
    }
}

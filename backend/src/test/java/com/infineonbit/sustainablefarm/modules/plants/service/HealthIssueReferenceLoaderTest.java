package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.HealthIssueReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The loader runs against a real database. It is built by hand, not imported as
 * a bean: a bean would already run at context startup, before the test.
 */
@DataJpaTest
@ActiveProfiles("test")
public class HealthIssueReferenceLoaderTest {

    private static final Instant FIRST_START = Instant.parse("2026-09-28T10:00:00Z");
    private static final Instant SECOND_START = Instant.parse("2026-10-05T08:00:00Z");

    @Autowired
    private HealthIssueReferenceRepository healthIssueReferenceRepository;

    private HealthIssueReferenceLoader loader;

    @BeforeEach
    void setUp() {
        loader = new HealthIssueReferenceLoader(healthIssueReferenceRepository);
    }

    private Map<String, HealthIssueReference> issuesByCode() {
        return healthIssueReferenceRepository.findAll().stream()
                .collect(Collectors.toMap(HealthIssueReference::getCode, issue -> issue));
    }

    private static void assertIssue(HealthIssueReference issue, String name, HealthIssueKind kind,
                                    String scientificName, String eppoCode, String source) {
        assertEquals(name, issue.getName());
        assertEquals(kind, issue.getKind());
        assertEquals(scientificName, issue.getScientificName());
        assertEquals(eppoCode, issue.getEppoCode());
        assertEquals(source, issue.getSource());
        assertEquals(FIRST_START, issue.getLastUpdated());
    }

    @Test
    void load_shouldInsertTheEightIssues_whenTableIsEmpty() {
        // Act
        loader.load(FIRST_START);
        // Assert: every row, with its kind, EPPO code and source
        Map<String, HealthIssueReference> issues = issuesByCode();
        assertEquals(8, issues.size());
        assertIssue(issues.get("FRUIT_FLY"), "Fruit flies", HealthIssueKind.PEST,
                "Tephritidae, mainly Bactrocera dorsalis and Ceratitis cosyra", null, "Zida_2023");
        assertIssue(issues.get("MANGO_MEALYBUG"), "Mango mealybug", HealthIssueKind.PEST,
                "Rastrococcus invadens", "RASTIN", "Vayssieres_2012");
        assertIssue(issues.get("TERMITES"), "Termites", HealthIssueKind.PEST, null, null, "Vayssieres_2012");
        assertIssue(issues.get("SCALE_INSECTS"), "Scale insects", HealthIssueKind.PEST, null, null,
                "Vayssieres_2012");
        assertIssue(issues.get("ANTHRACNOSE"), "Anthracnose", HealthIssueKind.DISEASE,
                "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025");
        assertIssue(issues.get("BACTERIAL_BLACK_SPOT"), "Bacterial black spot", HealthIssueKind.DISEASE,
                "Xanthomonas citri pv. mangiferaeindicae", "XANTMI", "Dianda_2025");
        assertIssue(issues.get("MANGO_DECLINE"), "Mango decline", HealthIssueKind.DISEASE, null, null,
                "Dianda_2025");
        assertIssue(issues.get("OTHER"), "Other", HealthIssueKind.OTHER, null, null, "by_definition");
    }

    @Test
    void load_shouldNotDuplicateTheIssues_whenRunTwice() {
        // Act
        loader.load(FIRST_START);
        loader.load(SECOND_START);
        // Assert: same rows, still dated from the first start
        assertEquals(8, healthIssueReferenceRepository.count());
        healthIssueReferenceRepository.findAll()
                .forEach(issue -> assertEquals(FIRST_START, issue.getLastUpdated()));
    }

    @Test
    void load_shouldKeepAnIssueChangedInTheDatabase() {
        // Arrange: a mentor renames an issue and removes its EPPO code
        loader.load(FIRST_START);
        HealthIssueReference mealybug = issuesByCode().get("MANGO_MEALYBUG");
        mealybug.setName("Mealybug");
        mealybug.setEppoCode(null);
        healthIssueReferenceRepository.save(mealybug);
        // Act
        loader.load(SECOND_START);
        // Assert: the changed values stay, nothing is added
        HealthIssueReference kept = issuesByCode().get("MANGO_MEALYBUG");
        assertEquals("Mealybug", kept.getName());
        assertNull(kept.getEppoCode());
        assertEquals(FIRST_START, kept.getLastUpdated());
        assertEquals(8, healthIssueReferenceRepository.count());
    }

    @Test
    void load_shouldInsertOnlyTheMissingCodes() {
        // Arrange: an issue deleted by hand
        loader.load(FIRST_START);
        healthIssueReferenceRepository.delete(issuesByCode().get("TERMITES"));
        // Act
        loader.load(SECOND_START);
        // Assert: it is back, dated from the second start; the others are untouched
        Map<String, HealthIssueReference> issues = issuesByCode();
        assertEquals(8, issues.size());
        assertEquals(SECOND_START, issues.get("TERMITES").getLastUpdated());
        assertEquals(FIRST_START, issues.get("ANTHRACNOSE").getLastUpdated());
    }
}

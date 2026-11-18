package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The loader runs against a real database. It is built by hand, not imported as
 * a bean: a bean would already run at context startup, before the test.
 */
@DataJpaTest
@ActiveProfiles("test")
public class AgronomicReferenceLoaderTest {

    private static final Instant FIRST_START = Instant.parse("2026-09-24T10:00:00Z");

    @Autowired
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    private AgronomicReferenceLoader loader;

    @BeforeEach
    void setUp() {
        loader = new AgronomicReferenceLoader(growthPhaseYieldShareRepository);
    }

    private List<String> shareRows() {
        return growthPhaseYieldShareRepository.findAll().stream()
                .map(share -> share.getGrowthPhase() + " " + share.getYieldShare() + " " + share.getSource())
                .sorted()
                .toList();
    }

    private void shareRow(String growthPhase, double yieldShare, String source) {
        growthPhaseYieldShareRepository.save(new GrowthPhaseYieldShare(null, growthPhase, yieldShare, source,
                FIRST_START));
    }

    @Test
    void removeFormerShareRows_shouldRemoveTheShareRows_writtenByEarlierVersions() {
        // Arrange: a dev database started before the shares had defaults in code
        shareRow("establishment", 0.0, "orchard_literature");
        shareRow("gradual production", 0.5, "assumption_to_validate");
        shareRow("full production", 1.0, "by_definition");
        // Act
        loader.removeFormerShareRows();
        // Assert: the defaults apply, gradual production at 0.25
        assertEquals(List.of(), shareRows());
    }

    @Test
    void removeFormerShareRows_shouldKeepEveryShareRow_thatDiffersFromAnEarlierVersion() {
        // Arrange: gradual production corrected, full production re-sourced, establishment as written before
        shareRow("establishment", 0.0, "orchard_literature");
        shareRow("gradual production", 0.3, "assumption_to_validate");
        shareRow("full production", 1.0, "user_entry");
        // Act
        loader.removeFormerShareRows();
        // Assert: only the row identical to an earlier version is removed
        assertEquals(List.of("full production 1.0 user_entry", "gradual production 0.3 assumption_to_validate"),
                shareRows());
    }
}

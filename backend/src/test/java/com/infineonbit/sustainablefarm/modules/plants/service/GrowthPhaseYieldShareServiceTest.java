package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrowthPhaseYieldShareServiceTest {

    private static final Instant CORRECTED = Instant.parse("2026-10-01T08:00:00Z");

    private static final GrowthPhaseYieldShareResponse ESTABLISHMENT_DEFAULT = new GrowthPhaseYieldShareResponse(
            "ESTABLISHMENT", "establishment", "0–2 yrs", 0.0, "orchard_literature", false,
            0.0, "orchard_literature", null);
    private static final GrowthPhaseYieldShareResponse GRADUAL_DEFAULT = new GrowthPhaseYieldShareResponse(
            "GRADUAL_PRODUCTION", "gradual production", "3–5 yrs", 0.25, "Bally_2002", false,
            0.25, "Bally_2002", null);
    private static final GrowthPhaseYieldShareResponse FULL_DEFAULT = new GrowthPhaseYieldShareResponse(
            "FULL_PRODUCTION", "full production", "6+ yrs", 1.0, "by_definition", false,
            1.0, "by_definition", null);

    @Mock
    private GrowthPhaseYieldShareRepository growthPhaseYieldShareRepository;

    @InjectMocks
    private GrowthPhaseYieldShareService growthPhaseYieldShareService;

    /** Stubs the rows of the table, each a correction. */
    private void corrections(GrowthPhaseYieldShare... rows) {
        when(growthPhaseYieldShareRepository.findAll()).thenReturn(List.of(rows));
    }

    @Test
    void getAllShares_shouldGiveTheSourcedDefaults_whenNoShareIsCorrected() {
        // Arrange
        corrections();
        // Act & Assert: the youngest phase first
        assertEquals(List.of(ESTABLISHMENT_DEFAULT, GRADUAL_DEFAULT, FULL_DEFAULT),
                growthPhaseYieldShareService.getAllShares());
    }

    @Test
    void getAllShares_shouldGiveTheCorrection_withTheDefaultBesideIt() {
        // Arrange: an agronomist corrected gradual production
        corrections(new GrowthPhaseYieldShare(7L, "gradual production", 0.3, "agronomist_visit_2026", CORRECTED));
        // Act & Assert: the other phases keep their defaults
        assertEquals(List.of(ESTABLISHMENT_DEFAULT,
                        new GrowthPhaseYieldShareResponse("GRADUAL_PRODUCTION", "gradual production", "3–5 yrs",
                                0.3, "agronomist_visit_2026", true, 0.25, "Bally_2002", CORRECTED),
                        FULL_DEFAULT),
                growthPhaseYieldShareService.getAllShares());
    }

    @Test
    void getAllShares_shouldIgnoreARowWhoseLabelIsNoPhase() {
        // Arrange: a row written by hand under a label the calculator never returns
        corrections(new GrowthPhaseYieldShare(8L, "adult", 0.8, "user_entry", CORRECTED));
        // Act & Assert
        assertEquals(List.of(ESTABLISHMENT_DEFAULT, GRADUAL_DEFAULT, FULL_DEFAULT),
                growthPhaseYieldShareService.getAllShares());
    }

    @Test
    void getAllShares_shouldGiveAShare_forThePhaseOfEveryAge() {
        // Arrange
        corrections();
        // Act
        Map<String, GrowthPhaseYieldShareResponse> sharesByPhase = growthPhaseYieldShareService.getAllShares().stream()
                .collect(Collectors.toMap(GrowthPhaseYieldShareResponse::growthPhase, share -> share));
        // Assert: the forecast finds a share for any tree up to 60 years old
        IntStream.rangeClosed(0, 60)
                .mapToObj(years -> GrowthPhaseCalculator.computePhase(Period.of(years, 11, 30)))
                .forEach(phase -> assertTrue(sharesByPhase.containsKey(phase), phase));
    }
}

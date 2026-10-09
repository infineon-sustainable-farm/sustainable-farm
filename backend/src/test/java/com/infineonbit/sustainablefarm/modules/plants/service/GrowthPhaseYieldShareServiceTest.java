package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.GrowthPhaseYieldShareRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.GrowthPhaseYieldShareResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.GrowthPhaseYieldShare;
import com.infineonbit.sustainablefarm.modules.plants.exception.GrowthPhaseNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.GrowthPhaseYieldShareRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrowthPhaseYieldShareServiceTest {

    private static final Instant CORRECTED = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");

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

    private void noCorrection(String growthPhase) {
        when(growthPhaseYieldShareRepository.findByGrowthPhase(growthPhase)).thenReturn(Optional.empty());
    }

    private void insertAssignsId(long id) {
        when(growthPhaseYieldShareRepository.saveAndFlush(any(GrowthPhaseYieldShare.class))).thenAnswer(invocation -> {
            GrowthPhaseYieldShare row = invocation.getArgument(0);
            row.setId(id);
            return row;
        });
    }

    private GrowthPhaseYieldShare insertedRow() {
        ArgumentCaptor<GrowthPhaseYieldShare> captor = ArgumentCaptor.forClass(GrowthPhaseYieldShare.class);
        verify(growthPhaseYieldShareRepository).saveAndFlush(captor.capture());
        return captor.getValue();
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

    @Test
    void saveShare_shouldAddTheFirstCorrection_asAUserEntry_whenNoSourceIsSent() {
        // Arrange
        noCorrection("gradual production");
        insertAssignsId(1L);
        // Act
        GrowthPhaseYieldShareResponse saved = growthPhaseYieldShareService.saveShare(
                "GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.3, null), NOW);
        // Assert: the row holds the label the forecast looks up
        GrowthPhaseYieldShare inserted = insertedRow();
        assertEquals("gradual production", inserted.getGrowthPhase());
        assertEquals(0.3, inserted.getYieldShare());
        assertEquals("user_entry", inserted.getSource());
        assertEquals(NOW, inserted.getLastUpdated());
        assertEquals(new GrowthPhaseYieldShareResponse("GRADUAL_PRODUCTION", "gradual production", "3–5 yrs",
                0.3, "user_entry", true, 0.25, "Bally_2002", NOW), saved);
    }

    @Test
    void saveShare_shouldReplaceTheCorrection_inItsOwnRow_withItsSourceTrimmed() {
        // Arrange
        GrowthPhaseYieldShare recorded = new GrowthPhaseYieldShare(
                7L, "gradual production", 0.3, "user_entry", CORRECTED);
        when(growthPhaseYieldShareRepository.findByGrowthPhase("gradual production"))
                .thenReturn(Optional.of(recorded));
        when(growthPhaseYieldShareRepository.save(recorded)).thenReturn(recorded);
        // Act
        GrowthPhaseYieldShareResponse saved = growthPhaseYieldShareService.saveShare(
                "GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.35, " agronomist_visit_2026 "), NOW);
        // Assert: the same row, share, source and date changed together; no second row
        assertEquals(7L, recorded.getId());
        assertEquals(0.35, recorded.getYieldShare());
        assertEquals("agronomist_visit_2026", recorded.getSource());
        assertEquals(NOW, recorded.getLastUpdated());
        assertEquals(new GrowthPhaseYieldShareResponse("GRADUAL_PRODUCTION", "gradual production", "3–5 yrs",
                0.35, "agronomist_visit_2026", true, 0.25, "Bally_2002", NOW), saved);
        verify(growthPhaseYieldShareRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveShare_shouldRecordAUserEntry_whenTheSourceIsBlank() {
        // Arrange
        noCorrection("establishment");
        insertAssignsId(2L);
        // Act & Assert
        assertEquals("user_entry", growthPhaseYieldShareService.saveShare(
                "ESTABLISHMENT", new GrowthPhaseYieldShareRequest(0.05, "   "), NOW).source());
    }

    @Test
    void saveShare_shouldFindThePhase_ignoringCaseAndSurroundingSpaces() {
        // Arrange
        noCorrection("full production");
        insertAssignsId(3L);
        // Act
        GrowthPhaseYieldShareResponse saved = growthPhaseYieldShareService.saveShare(
                " full_production ", new GrowthPhaseYieldShareRequest(0.9, null), NOW);
        // Assert
        assertEquals("FULL_PRODUCTION", saved.code());
        assertEquals("full production", insertedRow().getGrowthPhase());
    }

    @Test
    void saveShare_shouldThrowNotFound_forAnUnknownCode() {
        // Act
        GrowthPhaseNotFoundException exception = assertThrows(GrowthPhaseNotFoundException.class,
                () -> growthPhaseYieldShareService.saveShare(
                        " adult ", new GrowthPhaseYieldShareRequest(0.8, null), NOW));
        // Assert: the codes are listed; nothing is read nor written
        assertEquals("No growth phase with code adult. "
                + "The codes are ESTABLISHMENT, GRADUAL_PRODUCTION and FULL_PRODUCTION", exception.getMessage());
        verify(growthPhaseYieldShareRepository, never()).findByGrowthPhase(anyString());
        verify(growthPhaseYieldShareRepository, never()).saveAndFlush(any());
    }

    @Test
    void saveShare_shouldThrowConflict_whenAnotherFirstCorrectionIsRecordedAtTheSameTime() {
        // Arrange: no row when read, but the unique constraint refuses the insert
        noCorrection("gradual production");
        when(growthPhaseYieldShareRepository.saveAndFlush(any(GrowthPhaseYieldShare.class)))
                .thenThrow(new DataIntegrityViolationException("uk_growth_phase_yield_share_growth_phase"));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> growthPhaseYieldShareService.saveShare(
                        "GRADUAL_PRODUCTION", new GrowthPhaseYieldShareRequest(0.3, null), NOW));
        // Assert
        assertEquals("Another yield share for the gradual production phase was being recorded at the same time. "
                + "Nothing was saved: please send the request again.", exception.getMessage());
    }
}

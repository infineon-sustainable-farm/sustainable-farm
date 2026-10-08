package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.VarietyReferenceRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.VarietyReferenceResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyReferenceNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class VarietyReferenceServiceTest {

    private static final Instant ENTERED = Instant.parse("2026-10-01T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final String KEITT_SEASON_SOURCE = "varietal_guide_west_africa";

    @Mock
    private VarietyReferenceRepository varietyReferenceRepository;

    @InjectMocks
    private VarietyReferenceService varietyReferenceService;

    private static VarietyReference reference(long id, String name) {
        return new VarietyReference(id, name, 200.0, "Zalka_2025", 4, 5, "FAO_mango_burkina", ENTERED);
    }

    /** Keitt as the dev loader enters it: 220 kg from Zalka 2025, May to July. */
    private static VarietyReference keitt() {
        return new VarietyReference(1L, "Keitt", 220.0, "Zalka_2025", 5, 7, KEITT_SEASON_SOURCE, ENTERED);
    }

    /** Stubs the rows of the reference. */
    private void stored(VarietyReference... rows) {
        when(varietyReferenceRepository.findAll()).thenReturn(List.of(rows));
    }

    /** Stubs the reference being corrected, among the rows of the reference. */
    private VarietyReference correcting(VarietyReference recorded, VarietyReference... others) {
        when(varietyReferenceRepository.findById(recorded.getId())).thenReturn(Optional.of(recorded));
        VarietyReference[] rows = new VarietyReference[others.length + 1];
        rows[0] = recorded;
        System.arraycopy(others, 0, rows, 1, others.length);
        stored(rows);
        when(varietyReferenceRepository.saveAndFlush(recorded)).thenReturn(recorded);
        return recorded;
    }

    private void insertAssignsId(long id) {
        when(varietyReferenceRepository.saveAndFlush(any(VarietyReference.class))).thenAnswer(invocation -> {
            VarietyReference reference = invocation.getArgument(0);
            reference.setId(id);
            return reference;
        });
    }

    private VarietyReference writtenRow() {
        ArgumentCaptor<VarietyReference> captor = ArgumentCaptor.forClass(VarietyReference.class);
        verify(varietyReferenceRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    @Test
    void getAllReferences_shouldOrderByName_ignoringCaseAndAccents_thenById() {
        // Arrange
        stored(reference(3L, "kent"), reference(1L, "Keitt"), reference(4L, "Amélie"), reference(2L, "amelie"));
        // Act & Assert: the same order on every database, whatever its collation
        assertEquals(List.of("amelie", "Amélie", "Keitt", "kent"), varietyReferenceService.getAllReferences()
                .stream().map(VarietyReferenceResponse::varietyName).toList());
    }

    @Test
    void getAllReferences_shouldBeEmpty_whenNothingIsEntered() {
        // Arrange
        stored();
        // Act & Assert
        assertEquals(List.of(), varietyReferenceService.getAllReferences());
    }

    @Test
    void createReference_shouldStoreTheTrimmedName_withUserEntrySources_whenNoneIsSent() {
        // Arrange
        stored();
        insertAssignsId(1L);
        // Act
        VarietyReferenceResponse created = varietyReferenceService.createReference(
                new VarietyReferenceRequest(" Keitt ", 220.0, null, 5, 7, "  "), NOW);
        // Assert
        VarietyReference inserted = writtenRow();
        assertEquals("Keitt", inserted.getVarietyName());
        assertEquals("user_entry", inserted.getYieldSource());
        assertEquals("user_entry", inserted.getSeasonSource());
        assertEquals(new VarietyReferenceResponse(1L, "Keitt", 220.0, "user_entry", 5, 7, "user_entry", NOW),
                created);
    }

    @Test
    void createReference_shouldKeepTheSourcesSent_trimmed() {
        // Arrange
        stored();
        insertAssignsId(1L);
        // Act
        VarietyReferenceResponse created = varietyReferenceService.createReference(new VarietyReferenceRequest(
                "Keitt", 220.0, " Zalka_2025 ", 5, 7, " " + KEITT_SEASON_SOURCE + " "), NOW);
        // Assert
        assertEquals(new VarietyReferenceResponse(1L, "Keitt", 220.0, "Zalka_2025", 5, 7, KEITT_SEASON_SOURCE, NOW),
                created);
    }

    @Test
    void createReference_shouldThrowConflict_forTheSameNameIgnoringCaseAccentsAndSpaces() {
        // Arrange
        stored(keitt());
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> varietyReferenceService.createReference(
                        new VarietyReferenceRequest(" keítt ", 200.0, null, 4, 6, null), NOW));
        // Assert: the message names the reference already there
        assertEquals("A variety reference named Keitt already exists", exception.getMessage());
        verify(varietyReferenceRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReference_shouldThrowConflict_whenTheSameNameIsEnteredAtTheSameTime() {
        // Arrange: no row when read, but the unique constraint refuses the insert
        stored();
        when(varietyReferenceRepository.saveAndFlush(any(VarietyReference.class)))
                .thenThrow(new DataIntegrityViolationException("uk_variety_reference_variety_name"));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> varietyReferenceService.createReference(
                        new VarietyReferenceRequest("Keitt", 220.0, null, 5, 7, null), NOW));
        // Assert
        assertEquals("A variety reference named Keitt already exists", exception.getMessage());
    }

    @Test
    void updateReference_shouldReplaceEveryValue_inTheSameRow() {
        // Arrange
        VarietyReference recorded = correcting(keitt());
        // Act
        VarietyReferenceResponse corrected = varietyReferenceService.updateReference(1L,
                new VarietyReferenceRequest("Keitt", 230.0, "farm_records_2026", 4, 7, "farm_records_2026"), NOW);
        // Assert
        assertSame(recorded, writtenRow());
        assertEquals(new VarietyReferenceResponse(1L, "Keitt", 230.0, "farm_records_2026", 4, 7,
                "farm_records_2026", NOW), corrected);
    }

    @Test
    void updateReference_shouldKeepTheYieldSource_ofAnUnchangedYield_andCreditAChangedSeasonToTheUser() {
        // Arrange
        correcting(keitt());
        // Act: the months change, the yield does not; no source sent
        VarietyReferenceResponse corrected = varietyReferenceService.updateReference(1L,
                new VarietyReferenceRequest("Keitt", 220.0, null, 4, 7, null), NOW);
        // Assert
        assertEquals("Zalka_2025", corrected.yieldSource());
        assertEquals("user_entry", corrected.seasonSource());
    }

    @Test
    void updateReference_shouldCreditAChangedYieldToTheUser_andKeepTheSourceOfAnUnchangedSeason() {
        // Arrange
        correcting(keitt());
        // Act: the yield changes, the months do not; blank sources
        VarietyReferenceResponse corrected = varietyReferenceService.updateReference(1L,
                new VarietyReferenceRequest("Keitt", 230.0, " ", 5, 7, ""), NOW);
        // Assert
        assertEquals("user_entry", corrected.yieldSource());
        assertEquals(KEITT_SEASON_SOURCE, corrected.seasonSource());
    }

    @Test
    void updateReference_shouldCreditTheSeasonToTheUser_whenOnlyItsEndMonthChanges() {
        // Arrange
        correcting(keitt());
        // Act
        VarietyReferenceResponse corrected = varietyReferenceService.updateReference(1L,
                new VarietyReferenceRequest("Keitt", 220.0, null, 5, 8, null), NOW);
        // Assert
        assertEquals("user_entry", corrected.seasonSource());
        assertEquals(NOW, corrected.lastUpdated());
    }

    @Test
    void updateReference_shouldLetAReferenceChangeTheCaseOfItsOwnName() {
        // Arrange
        correcting(keitt(), reference(2L, "Kent"));
        // Act
        VarietyReferenceResponse corrected = varietyReferenceService.updateReference(1L,
                new VarietyReferenceRequest(" KEITT ", 220.0, null, 5, 7, null), NOW);
        // Assert
        assertEquals("KEITT", corrected.varietyName());
    }

    @Test
    void updateReference_shouldThrowConflict_whenRenamedAfterAnotherVariety() {
        // Arrange
        when(varietyReferenceRepository.findById(1L)).thenReturn(Optional.of(keitt()));
        stored(keitt(), reference(2L, "Kent"));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> varietyReferenceService.updateReference(1L,
                        new VarietyReferenceRequest("kent", 220.0, null, 5, 7, null), NOW));
        // Assert
        assertEquals("A variety reference named Kent already exists", exception.getMessage());
        verify(varietyReferenceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateReference_shouldThrowNotFound_forAnUnknownId() {
        // Arrange
        when(varietyReferenceRepository.findById(99L)).thenReturn(Optional.empty());
        // Act
        VarietyReferenceNotFoundException exception = assertThrows(VarietyReferenceNotFoundException.class,
                () -> varietyReferenceService.updateReference(99L,
                        new VarietyReferenceRequest("Keitt", 220.0, null, 5, 7, null), NOW));
        // Assert
        assertEquals("Variety reference with ID 99 not found", exception.getMessage());
    }

    @Test
    void updateReference_shouldThrowConflict_whenTheSameNameIsWrittenAtTheSameTime() {
        // Arrange: Kent renamed "Sensation" while another request enters "Sensation"
        when(varietyReferenceRepository.findById(2L)).thenReturn(Optional.of(reference(2L, "Kent")));
        stored(keitt(), reference(2L, "Kent"));
        when(varietyReferenceRepository.saveAndFlush(any(VarietyReference.class)))
                .thenThrow(new DataIntegrityViolationException("uk_variety_reference_variety_name"));
        // Act
        ConflictException exception = assertThrows(ConflictException.class,
                () -> varietyReferenceService.updateReference(2L,
                        new VarietyReferenceRequest("Sensation", 200.0, null, 4, 5, null), NOW));
        // Assert
        assertEquals("A variety reference named Sensation already exists", exception.getMessage());
    }
}

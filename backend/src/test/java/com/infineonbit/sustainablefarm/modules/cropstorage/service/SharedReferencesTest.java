package com.infineonbit.sustainablefarm.modules.cropstorage.service;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestLookup;
import com.infineonbit.sustainablefarm.modules.plants.api.HarvestSummary;
import com.infineonbit.sustainablefarm.modules.plants.api.VarietyCatalog;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The checks on concepts of other modules, through their api packages (mocked). */
class SharedReferencesTest {

    private final VarietyCatalog varieties = mock(VarietyCatalog.class);
    private final HarvestLookup harvests = mock(HarvestLookup.class);
    private final PersonDirectory people = mock(PersonDirectory.class, CALLS_REAL_METHODS);
    private final SharedReferences references = new SharedReferences(varieties, harvests, people);

    @Test
    void aVarietyNameIsStoredAsItsCatalogueKeyAndAnUnknownNameIsABadRequest() {
        when(varieties.resolve("Kent")).thenReturn(Optional.of(new CatalogVariety("kent", "Kent")));
        when(varieties.resolve("Imaginary")).thenReturn(Optional.empty());

        assertEquals("kent", references.varietyKeyOf("Kent"));
        assertThrows(BadRequestException.class, () -> references.varietyKeyOf("Imaginary"));
    }

    @Test
    void noVarietyNameMeansNoVarietyWithoutAskingTheCatalogue() {
        assertNull(references.varietyKeyOf(null));
        assertNull(references.varietyKeyOf("  "));
        verifyNoInteractions(varieties);
    }

    @Test
    void aHarvestMustBeKnownToPlants() {
        when(harvests.findHarvest(7L)).thenReturn(Optional.of(mock(HarvestSummary.class)));
        when(harvests.findHarvest(8L)).thenReturn(Optional.empty());

        assertEquals(7L, references.requireHarvest(7L));
        assertThrows(BadRequestException.class, () -> references.requireHarvest(8L));
        assertNull(references.requireHarvest(null));
    }

    @Test
    void anInspectorMustBeAPersonOfTheStaffDirectory() {
        when(people.exists(3L)).thenReturn(true);
        when(people.exists(4L)).thenReturn(false);

        assertEquals(3L, references.requirePerson(3L));
        assertThrows(BadRequestException.class, () -> references.requirePerson(4L));
        assertNull(references.requirePerson(null));
    }
}

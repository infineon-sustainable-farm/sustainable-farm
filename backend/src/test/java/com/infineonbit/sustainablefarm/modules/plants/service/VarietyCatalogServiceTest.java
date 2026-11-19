package com.infineonbit.sustainablefarm.modules.plants.service;

import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.api.CatalogVariety;
import com.infineonbit.sustainablefarm.modules.plants.entity.VarietyReference;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VarietyCatalogServiceTest {

    private final VarietyReferenceRepository repository = mock(VarietyReferenceRepository.class);
    private final VarietyCatalogService catalog = new VarietyCatalogService(repository);

    {
        when(repository.findAll()).thenReturn(VarietyCatalogTestData.defaults());
    }

    @Test
    void listsTheCatalogueByName() {
        assertThat(catalog.listVarieties()).extracting(CatalogVariety::name)
                .containsExactly("Amelie", "Keitt", "Kent", "Other", "Tommy Atkins");
    }

    @Test
    void resolvesAKeyOrANameIgnoringCaseAccentsAndSeparators() {
        assertThat(catalog.resolve("keitt")).map(CatalogVariety::key).contains("KEITT");
        assertThat(catalog.resolve(" Amélie ")).map(CatalogVariety::key).contains("AMELIE");
        assertThat(catalog.resolve("AMÉLIE")).map(CatalogVariety::key).contains("AMELIE");
        assertThat(catalog.resolve("TOMMY")).map(CatalogVariety::key).contains("TOMMY");
        assertThat(catalog.resolve("tommy-atkins")).map(CatalogVariety::name).contains("Tommy Atkins");
    }

    @Test
    void resolvesNothingForAnUnknownOrMissingName() {
        assertThat(catalog.resolve("Alphonso")).isEmpty();
        assertThat(catalog.resolve(null)).isEmpty();
        assertThat(catalog.resolve("  ")).isEmpty();
    }

    @Test
    void findsByNameIgnoringCaseAndAccents() {
        assertThat(catalog.findByName("amélie")).map(CatalogVariety::key).contains("AMELIE");
        assertThat(catalog.findByName("TOMMY")).isEmpty();
    }

    @Test
    void findsByExactKey() {
        VarietyReference kent = VarietyCatalogTestData.defaults().get(1);
        when(repository.findByVarietyKey("KENT")).thenReturn(Optional.of(kent));

        assertThat(catalog.findByKey("KENT")).contains(new CatalogVariety("KENT", "Kent"));
        assertThat(catalog.findByKey(null)).isEmpty();
    }

    @Test
    void derivesAKeyFromAName() {
        assertThat(VarietyReference.keyOf(" Tommy Atkins ")).isEqualTo("TOMMY_ATKINS");
        assertThat(VarietyReference.keyOf("Amélie")).isEqualTo("AMELIE");
        assertThat(VarietyReference.keyOf("Mallika (local)")).isEqualTo("MALLIKA_LOCAL");
    }
}

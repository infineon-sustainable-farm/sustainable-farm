package com.infineonbit.sustainablefarm.modules.producttransformation.variety;

import com.infineonbit.sustainablefarm.core.exception.BadRequestException;
import com.infineonbit.sustainablefarm.modules.plants.VarietyCatalogTestData;
import com.infineonbit.sustainablefarm.modules.plants.repository.VarietyReferenceRepository;
import com.infineonbit.sustainablefarm.modules.plants.service.VarietyCatalogService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** The API values of producttransformation stay those of the former enum. */
class MangoVarietyCodesTest {

    private final MangoVarietyCodes codes = codes();

    private static MangoVarietyCodes codes() {
        VarietyReferenceRepository repository = mock(VarietyReferenceRepository.class);
        when(repository.findAll()).thenReturn(VarietyCatalogTestData.defaults());
        return new MangoVarietyCodes(new VarietyCatalogService(repository));
    }

    @Test
    void everyFormerEnumValueIsAcceptedAndSentBackUnchanged() {
        for (String value : new String[] {"KEITT", "KENT", "TOMMY", "AMÉLIE", "OTHER"}) {
            assertThat(MangoVarietyCodes.toApi(codes.toKey(value))).isEqualTo(value);
        }
    }

    @Test
    void ameliesKeyIsAccentFree() {
        assertThat(codes.toKey("AMÉLIE")).isEqualTo("AMELIE");
        assertThat(MangoVarietyCodes.toApi("AMELIE")).isEqualTo("AMÉLIE");
    }

    @Test
    void unknownVarietyIsABadRequest() {
        assertThatThrownBy(() -> codes.toKey("ALPHONSO")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void nullStaysNull() {
        assertThat(codes.toKey(null)).isNull();
        assertThat(MangoVarietyCodes.toApi(null)).isNull();
    }
}

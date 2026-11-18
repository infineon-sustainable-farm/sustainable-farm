package com.infineonbit.sustainablefarm.modules.watersupply.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class StoredValueTest {

    @Test
    void parsesAValueIgnoringCaseAndSpaces() {
        assertThat(IrrigationStatus.of(" Scheduled ")).isEqualTo(IrrigationStatus.SCHEDULED);
        assertThat(UnavailabilityScope.of("ZONE")).isEqualTo(UnavailabilityScope.ZONE);
        assertThat(QuotaTargetType.of(null)).isNull();
        assertThat(IrrigationTrigger.of("  ")).isNull();
    }

    @Test
    void refusesAnUnknownValueListingTheExpectedOnes() {
        assertThatThrownBy(() -> UnavailabilityStatus.of("done"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid status: planned, active, cancelled expected.");
    }

    @Test
    void storesAndReadsBackTheLowerCaseValue() {
        IotCommandStatus.Converter converter = new IotCommandStatus.Converter();
        assertThat(converter.convertToDatabaseColumn(IotCommandStatus.REJECTED)).isEqualTo("rejected");
        assertThat(converter.convertToEntityAttribute("dispatched")).isEqualTo(IotCommandStatus.DISPATCHED);
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(IrrigationLogStatus.COMPLETED).hasToString("completed");
        assertThat(StoredValue.valueOf(null)).isNull();
    }
}

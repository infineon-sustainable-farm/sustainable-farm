package com.infineonbit.sustainablefarm.modules.producttransformation.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.infineonbit.sustainablefarm.modules.producttransformation.batch.model.Batch;
import com.infineonbit.sustainablefarm.modules.producttransformation.dryingrun.model.DryingRun;
import com.infineonbit.sustainablefarm.modules.producttransformation.washsortrecord.model.WashSortRecord;

/**
 * Precision tests for the BigDecimal-derived percentages and energy ratios.
 *
 * <p>These values feed operator dashboards and compliance reports, so the
 * rounding contract (four decimal places, HALF_UP, then x100 for percentages)
 * is pinned here instead of being inferred from floating-point output.</p>
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */
@DisplayName("Product Transformation BigDecimal calculations")
class BigDecimalPrecisionTest {

    @Test
    @DisplayName("wash yield rounds to four decimals before converting to a percentage")
    void washYieldUsesFourDecimalRounding() {
        WashSortRecord record = new WashSortRecord();
        record.setInputQuantityKg(new BigDecimal("520.00"));
        record.setOutputQuantityKg(new BigDecimal("480.00"));
        record.setWasteQuantityKg(new BigDecimal("40.00"));

        assertEquals(92.31, record.calculateYieldPercentage(), 0.000001);
        assertEquals(7.69, record.calculateWastePercentage(), 0.000001);
    }

    @Test
    @DisplayName("wash yield truncates the repeating decimal at four places, not at display time")
    void washYieldPinsFourDecimalScale() {
        WashSortRecord record = new WashSortRecord();
        record.setInputQuantityKg(new BigDecimal("3"));
        record.setOutputQuantityKg(BigDecimal.ONE);
        record.setWasteQuantityKg(new BigDecimal("2"));

        // 1/3 = 0.333333... -> 0.3333 (4 dp) -> 33.33, not 33.333333.
        assertEquals(33.33, record.calculateYieldPercentage(), 0.000001);
        assertEquals(66.67, record.calculateWastePercentage(), 0.000001);
    }

    @Test
    @DisplayName("wash percentages are zero for a null or zero input instead of dividing by zero")
    void washPercentagesGuardAgainstZeroInput() {
        WashSortRecord nullInput = new WashSortRecord();
        nullInput.setOutputQuantityKg(BigDecimal.TEN);
        nullInput.setWasteQuantityKg(BigDecimal.ONE);
        assertEquals(0.0, nullInput.calculateYieldPercentage(), 0.000001);
        assertEquals(0.0, nullInput.calculateWastePercentage(), 0.000001);

        WashSortRecord zeroInput = new WashSortRecord();
        zeroInput.setInputQuantityKg(BigDecimal.ZERO);
        zeroInput.setOutputQuantityKg(BigDecimal.TEN);
        zeroInput.setWasteQuantityKg(BigDecimal.ONE);
        assertEquals(0.0, zeroInput.calculateYieldPercentage(), 0.000001);
        assertEquals(0.0, zeroInput.calculateWastePercentage(), 0.000001);
    }

    @Test
    @DisplayName("drying moisture reduction uses the same four-decimal contract")
    void dryingMoistureReductionUsesFourDecimalRounding() {
        DryingRun run = new DryingRun();
        run.setStartMoisturePct(new BigDecimal("78.00"));
        run.setEndMoisturePct(new BigDecimal("12.00"));

        // 66/78 = 0.846153... -> 0.8462 -> 84.62
        assertEquals(84.62, run.calculateMoistureReductionPct(), 0.000001);
    }

    @Test
    @DisplayName("energy efficiency is kWh divided by the batch harvest quantity")
    void energyEfficiencyUsesBatchHarvestQuantity() {
        Batch batch = new Batch();
        batch.setHarvestQuantityKg(new BigDecimal("520.00"));

        DryingRun run = new DryingRun();
        run.setBatch(batch);
        run.setEnergyUsageKwh(new BigDecimal("118.40"));

        // 118.40/520 = 0.227692... -> 0.2277
        assertEquals(0.2277, run.calculateEnergyEfficiencyKwhPerKg(), 0.000001);
    }

    @Test
    @DisplayName("energy efficiency is zero when energy, batch or harvest quantity is missing")
    void energyEfficiencyGuardsAgainstMissingInputs() {
        DryingRun noEnergy = new DryingRun();
        noEnergy.setBatch(batchWithQuantity("520"));
        assertEquals(0.0, noEnergy.calculateEnergyEfficiencyKwhPerKg(), 0.000001);

        DryingRun noBatch = new DryingRun();
        noBatch.setEnergyUsageKwh(new BigDecimal("118.40"));
        assertEquals(0.0, noBatch.calculateEnergyEfficiencyKwhPerKg(), 0.000001);

        DryingRun noQuantity = new DryingRun();
        noQuantity.setEnergyUsageKwh(new BigDecimal("118.40"));
        noQuantity.setBatch(new Batch());
        assertEquals(0.0, noQuantity.calculateEnergyEfficiencyKwhPerKg(), 0.000001);

        DryingRun zeroQuantity = new DryingRun();
        zeroQuantity.setEnergyUsageKwh(new BigDecimal("118.40"));
        zeroQuantity.setBatch(batchWithQuantity("0"));
        assertEquals(0.0, zeroQuantity.calculateEnergyEfficiencyKwhPerKg(), 0.000001);
    }

    @Test
    @DisplayName("target moisture range is inclusive from 12 to 18 percent")
    void targetMoistureRangeIsInclusive() {
        assertTrue(runWithEndMoisture("12").isWithinTargetRange());
        assertTrue(runWithEndMoisture("18").isWithinTargetRange());

        assertFalse(runWithEndMoisture("11.99").isWithinTargetRange());
        assertFalse(runWithEndMoisture("18.01").isWithinTargetRange());

        DryingRun nullMoisture = new DryingRun();
        assertFalse(nullMoisture.isWithinTargetRange());
    }

    private static Batch batchWithQuantity(String quantityKg) {
        Batch batch = new Batch();
        batch.setHarvestQuantityKg(new BigDecimal(quantityKg));
        return batch;
    }

    private static DryingRun runWithEndMoisture(String endMoisturePct) {
        DryingRun run = new DryingRun();
        run.setEndMoisturePct(new BigDecimal(endMoisturePct));
        return run;
    }
}

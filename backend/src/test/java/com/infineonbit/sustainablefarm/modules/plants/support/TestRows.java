package com.infineonbit.sustainablefarm.modules.plants.support;

import com.infineonbit.sustainablefarm.modules.plants.entity.BlockLabel;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthFinding;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthInspection;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueReference;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthTreatment;
import com.infineonbit.sustainablefarm.modules.plants.entity.InspectionMethod;
import com.infineonbit.sustainablefarm.modules.plants.entity.TreatmentUnit;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Plants rows built in memory for the unit tests, in the order of their former all-arguments
 * constructors. The block of a code is the block {@link TestBlocks#idOf} of the farm given
 * ({@code null} for a row whose farm is not set).
 */
public final class TestRows {
    private TestRows() {
    }

    /** The block id and label of a code on a farm, or nothing for a {@code null} code. */
    private static BlockLabel label(Integer farmId, String blockCode) {
        return new BlockLabel(farmId == null ? null : farmId.longValue(), blockCode);
    }

    private static Long blockId(String blockCode) {
        return blockCode == null ? null : TestBlocks.idOf(blockCode);
    }

    public static HealthInspection inspection(Long id, Integer farmId, String blockCode, LocalDate inspectedOn,
                                              Integer healthScorePct, Long observerPersonId, InspectionMethod method,
                                              String source, Instant lastUpdated) {
        HealthInspection inspection = new HealthInspection();
        inspection.setId(id);
        inspection.assignBlock(blockId(blockCode), label(farmId, blockCode));
        inspection.setInspectedOn(inspectedOn);
        inspection.setHealthScorePct(healthScorePct);
        inspection.setObserverPersonId(observerPersonId);
        inspection.setMethod(method);
        inspection.setSource(source);
        inspection.setLastUpdated(lastUpdated);
        return inspection;
    }

    public static HealthTreatment treatment(Long id, Integer farmId, String blockCode, HealthIssueReference targetIssue,
                                            String targetOtherLabel, HealthFinding finding, LocalDate treatedOn,
                                            String productName, String activeIngredient, BigDecimal quantity,
                                            TreatmentUnit unit, Integer preHarvestIntervalDays, Long applicatorPersonId,
                                            String equipment, String source, Instant lastUpdated) {
        HealthTreatment treatment = new HealthTreatment();
        treatment.setId(id);
        treatment.assignBlock(blockId(blockCode), label(farmId, blockCode));
        treatment.setTargetIssue(targetIssue);
        treatment.setTargetOtherLabel(targetOtherLabel);
        treatment.setFinding(finding);
        treatment.setTreatedOn(treatedOn);
        treatment.setProductName(productName);
        treatment.setActiveIngredient(activeIngredient);
        treatment.setQuantity(quantity);
        treatment.setUnit(unit);
        treatment.setPreHarvestIntervalDays(preHarvestIntervalDays);
        treatment.setApplicatorPersonId(applicatorPersonId);
        treatment.setEquipment(equipment);
        treatment.setSource(source);
        treatment.setLastUpdated(lastUpdated);
        return treatment;
    }
}

package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Unit of the quantity of a {@link HealthTreatment}: kilograms for a powder,
 * litres for a liquid.
 *
 * <p>Kept apart from {@link FertilizerUnit}, although the values are the same:
 * each table has its own CHECK constraint, which {@code ddl-auto=update} does
 * not widen later, so every planned value is declared here from the start.
 */
public enum TreatmentUnit {
    KG,
    L
}

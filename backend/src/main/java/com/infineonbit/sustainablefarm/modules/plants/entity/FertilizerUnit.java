package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Unit in which a {@link FertilizerProduct} is counted: kilograms for solids,
 * litres for liquids. Every quantity of its movements and its reorder threshold
 * are in this unit.
 *
 * <p>Stored as text with a CHECK constraint, which {@code ddl-auto=update} does
 * not widen later: every planned value is declared here from the start.
 */
public enum FertilizerUnit {
    KG("kg"),
    L("L");

    private final String symbol;

    FertilizerUnit(String symbol) {
        this.symbol = symbol;
    }

    /** The unit as written after a quantity in a message, for example {@code "kg"}. */
    public String getSymbol() {
        return symbol;
    }
}

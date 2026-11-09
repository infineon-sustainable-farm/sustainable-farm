package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * How a {@link MaturityObservation} was made, with the unit of the value it
 * can measure.
 *
 * <ul>
 *     <li>{@code FLESH_COLOR}: the colour of the flesh near the stone, on a cut fruit;</li>
 *     <li>{@code BRIX}: the soluble solids of the juice, read with a refractometer, in °Bx;</li>
 *     <li>{@code DRY_MATTER}: the dry matter of the flesh, in percent;</li>
 *     <li>{@code VISUAL}: the look of the fruit on the tree, its shoulders and its skin.</li>
 * </ul>
 *
 * <p>Stored as text with a CHECK constraint, which {@code ddl-auto=update} does
 * not widen later: every planned value is declared here from the start.
 */
public enum MaturityMethod {
    FLESH_COLOR(null),
    BRIX("°Bx"),
    DRY_MATTER("%"),
    VISUAL(null);

    private final String unit;

    MaturityMethod(String unit) {
        this.unit = unit;
    }

    /** Unit of the value this method measures, {@code null} when it measures none. */
    public String unit() {
        return unit;
    }

    /** Whether an observation made this way can carry a measured value. */
    public boolean measuresAValue() {
        return unit != null;
    }
}

package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Kind of a {@link FertilizerProduct}.
 *
 * <p>MINERAL covers the manufactured fertilizers (NPK, urea); ORGANIC covers
 * manure and compost. GLOBALG.A.P. asks for the name and the type of every
 * fertilizer applied, and the distinction matters for an organic production.
 *
 * <p>Stored as text with a CHECK constraint, which {@code ddl-auto=update} does
 * not widen later: every planned value is declared here from the start.
 */
public enum FertilizerType {
    MINERAL,
    ORGANIC
}

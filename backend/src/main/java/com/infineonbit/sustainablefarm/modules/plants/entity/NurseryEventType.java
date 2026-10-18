package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Kind of fact recorded by a {@link NurseryEvent}.
 *
 * <p>STAGE_CHANGE sets the stage of the batch. LOSS and TRANSPLANT take plants
 * out of it: a loss because they died, a transplant because they went to the
 * orchard. Every value is declared from the start: the CHECK constraint of the
 * column is not widened by {@code ddl-auto=update}.
 */
public enum NurseryEventType {
    STAGE_CHANGE,
    LOSS,
    TRANSPLANT
}

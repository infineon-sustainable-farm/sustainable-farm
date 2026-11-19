package com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model;

/**
 * QcStage Enum
 * Business Rule: Matches database CHECK constraint
 */
public enum QcStage {
    INTAKE,
    WASHING,
    DRYING,
    COOLING,
    PACKAGING,
    FINAL
}

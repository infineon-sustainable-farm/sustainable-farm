package com.infineonbit.sustainablefarm.modules.producttransformation.harvestevent.model;

/**
 * Quality Grade Enum
 * Business Rule: Matches database CHECK constraint
 */
public enum QualityGrade {
    A,    // Premium - No defects
    B,    // Standard - Minor defects
    C,    // Processing - Visible defects
    D     // Reject - Significant defects
}

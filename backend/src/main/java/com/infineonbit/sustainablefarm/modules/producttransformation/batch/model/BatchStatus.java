package com.infineonbit.sustainablefarm.modules.producttransformation.batch.model;

/**
 * Batch Status Enum
 * Business Rule: Matches database CHECK constraint
 * Business Rule: Status transitions must follow the processing workflow
 */
public enum BatchStatus {
    CREATED,
    INTAKE,
    WASHING,
    DRYING,
    PACKAGING,
    COMPLETED,
    SHIPPED,
    REJECTED
}

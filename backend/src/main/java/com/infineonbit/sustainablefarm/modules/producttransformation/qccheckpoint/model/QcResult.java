package com.infineonbit.sustainablefarm.modules.producttransformation.qccheckpoint.model;

/**
 * QcResult Enum
 * Business Rule: Matches database CHECK constraint
 */
public enum QcResult {
    PENDING,
    PASS,
    FAIL,
    REWORK
}

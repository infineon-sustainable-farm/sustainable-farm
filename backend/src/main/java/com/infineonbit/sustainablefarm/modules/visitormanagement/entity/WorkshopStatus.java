package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Lifecycle of a workshop / tour template. Workshops are created as DRAFT,
 * become ACTIVE once published and can be deactivated (INACTIVE) when no
 * longer offered.
 */
public enum WorkshopStatus {
    DRAFT,
    ACTIVE,
    INACTIVE
}
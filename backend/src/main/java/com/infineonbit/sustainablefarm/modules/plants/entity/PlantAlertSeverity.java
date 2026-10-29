package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Severity of a plant alert, from the most to the least severe: the alerts
 * are listed in this order. An alert warns and never blocks anything.
 */
public enum PlantAlertSeverity {
    /** To deal with now: a harvest would break a pre-harvest interval, or a fertilizer is out of stock. */
    CRITICAL,
    /** To plan for. */
    WARNING
}

package com.infineonbit.sustainablefarm.modules.notification.api;

/** The module that raised an alert: each module reads and changes only its own alerts. */
public enum SourceModule {
    ENERGYSUPPLY,
    SALESMARKETING,
    WATERSUPPLY
}

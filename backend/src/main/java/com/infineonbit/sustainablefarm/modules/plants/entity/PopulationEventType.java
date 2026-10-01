package com.infineonbit.sustainablefarm.modules.plants.entity;

/**
 * Kind of change recorded by a {@link PopulationEvent}.
 *
 * <p>PLANTING, REPLACEMENT and EXTENSION add trees to a variety; MORTALITY and
 * REMOVAL take trees away. Only PLANTING is written for now. The other values
 * are declared already, so the stored model does not change when they get
 * their own routes.
 */
public enum PopulationEventType {
    PLANTING,
    MORTALITY,
    REPLACEMENT,
    EXTENSION,
    REMOVAL
}

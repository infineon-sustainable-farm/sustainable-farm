package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Lifecycle of an event. Events start as DRAFT, are PUBLISHED so that
 * visitors can register, can be CANCELLED, and end COMPLETED once the
 * date has passed.
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    CANCELLED,
    COMPLETED
}
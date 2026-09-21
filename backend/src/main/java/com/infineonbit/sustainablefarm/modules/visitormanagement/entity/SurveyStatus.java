package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Lifecycle of a post-visit survey sent to a visitor. A survey is SENT
 * as soon as it is dispatched by email/SMS/WhatsApp, and becomes RECEIVED
 * once the visitor's feedback response is linked to it.
 */
public enum SurveyStatus {
    SENT,
    RECEIVED
}
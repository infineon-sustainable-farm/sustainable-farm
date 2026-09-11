package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * How a feedback response was collected, and which channel a survey
 * was sent on. ON_SITE is reserved for the tablet wrap-up stop;
 * EMAIL / SMS / WHATSAPP are used for the post-visit survey link.
 */
public enum FeedbackChannel {
    ON_SITE,
    EMAIL,
    SMS,
    WHATSAPP
}
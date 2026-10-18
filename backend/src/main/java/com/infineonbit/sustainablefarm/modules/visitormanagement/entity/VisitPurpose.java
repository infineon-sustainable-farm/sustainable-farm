package com.infineonbit.sustainablefarm.modules.visitormanagement.entity;

/**
 * Why a visitor is coming to the farm.
 * Commercial purposes (PURCHASE, PARTNERSHIP, INVESTMENT) automatically
 * flag the registration as a prospect for the Sales & Marketing team.
 */
public enum VisitPurpose {
    TOURISM,
    PURCHASE,
    PARTNERSHIP,
    INVESTMENT,
    EDUCATION,
    OTHER
}

package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

/** Lifecycle stage of a batch (agreed with Anar): FRESH, then DRIED, then PACKAGED; WASTE or RETURNED at the end. */
public enum ProductStage {
    FRESH,
    DRIED,
    PACKAGED,
    WASTE,
    RETURNED
}

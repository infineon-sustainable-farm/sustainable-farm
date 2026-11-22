package com.infineonbit.sustainablefarm.modules.cropstorage.entity;

/** Whether a batch is still in the store or has left it. The remaining stock is read from its quantities. */
public enum BatchStatus {
    STORED,
    DISPATCHED
}

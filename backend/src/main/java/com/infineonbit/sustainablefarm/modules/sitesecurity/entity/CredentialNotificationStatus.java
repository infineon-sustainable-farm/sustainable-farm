package com.infineonbit.sustainablefarm.modules.sitesecurity.entity;

/** Result of the latest email about a credential. Empty when no email is known for it. */
public enum CredentialNotificationStatus {
    PENDING,
    SENT,
    FAILED
}

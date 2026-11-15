package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

/** Carries only the id, so that no personal data travels with the event. */
public record CredentialNotificationEvent(String credentialId, Type type) {

    public enum Type {
        GRANTED,
        REVOKED
    }
}

package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialNotificationStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Emails a credential holder once the change is committed, on another thread:
 * a mail failure never undoes or delays the change. The result is stored so
 * that the interface can show it. Logs carry the credential id, never the email.
 */
@Component
@RequiredArgsConstructor
public class CredentialNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(CredentialNotificationListener.class);
    private static final String REVOKED_STATUS = "Revoked";

    private final CredentialUserRepository credentialRepository;
    private final ZoneRepository zoneRepository;
    private final NotificationService notificationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCredentialNotification(CredentialNotificationEvent event) {
        CredentialUserEntity credential = credentialRepository.findById(event.credentialId()).orElse(null);
        if (credential == null || credential.getEmail() == null) {
            return;
        }
        boolean isGrant = event.type() == CredentialNotificationEvent.Type.GRANTED;
        if (isGrant && REVOKED_STATUS.equals(credential.getStatus())) {
            return;
        }

        CredentialNotificationStatus result = send(event, credential);
        if (isGrant) {
            credentialRepository.updateGrantNotificationStatus(credential.getId(), result);
        } else {
            credentialRepository.updateNotificationStatus(credential.getId(), result);
        }
    }

    private CredentialNotificationStatus send(CredentialNotificationEvent event, CredentialUserEntity credential) {
        try {
            String zoneName = zoneRepository.findById(credential.getLevel())
                    .map(ZoneEntity::getName)
                    .orElse(credential.getLevel());
            CredentialEmailBuilder.MailContent mail =
                    CredentialEmailBuilder.build(event.type(), credential, zoneName);
            if (notificationService.send(credential.getEmail(), mail.subject(), mail.html())) {
                return CredentialNotificationStatus.SENT;
            }
        } catch (RuntimeException e) {
            log.warn("Email error for credential {}: {}", credential.getId(), e.getClass().getSimpleName());
        }
        log.warn("The {} email for credential {} was not sent.", event.type(), credential.getId());
        return CredentialNotificationStatus.FAILED;
    }
}

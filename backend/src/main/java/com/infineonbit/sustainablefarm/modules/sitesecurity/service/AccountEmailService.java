package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.DeliveryStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.EmailKind;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.SubmissionStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.TokenPurpose;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.EmailDeliveryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.EmailDeliveryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountMailSender.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Account emails: queued in the transaction of the change, sent after it is
 * committed (a mail failure never undoes the change), retried on temporary
 * failures. Only token hashes are stored, so a retry of an email carrying a
 * link issues a fresh link (the old one stops working). Logs carry ids only.
 */
@Service
public class AccountEmailService {

    /** Delays before attempts 2 to 6; after that the email is marked FAILED. */
    static final List<Duration> RETRY_DELAYS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
            Duration.ofMinutes(15), Duration.ofHours(1), Duration.ofHours(4));
    /** A QUEUED or SENDING email older than this was interrupted (restart, crash). */
    static final Duration STUCK_AFTER = Duration.ofMinutes(10);

    private static final Logger log = LoggerFactory.getLogger(AccountEmailService.class);

    /** Published inside the change's transaction; the raw token lives only in memory. */
    public record QueuedEmail(String deliveryId, String rawToken) {
        @Override
        public String toString() {
            return "QueuedEmail[deliveryId=" + deliveryId + "]";
        }
    }

    private final EmailDeliveryRepository deliveries;
    private final OperatorRepository accounts;
    private final AccountTokenRepository tokens;
    private final AccountTokens accountTokens;
    private final AccountMailSender sender;
    private final AccountLinks links;
    private final SiteSecurityAuditService audit;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;

    public AccountEmailService(EmailDeliveryRepository deliveries, OperatorRepository accounts,
            AccountTokenRepository tokens, AccountTokens accountTokens, AccountMailSender sender, AccountLinks links,
            SiteSecurityAuditService audit, ApplicationEventPublisher events, PlatformTransactionManager transactionManager) {
        this.deliveries = deliveries;
        this.accounts = accounts;
        this.tokens = tokens;
        this.accountTokens = accountTokens;
        this.sender = sender;
        this.links = links;
        this.audit = audit;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** Records the email to send once the current transaction commits. */
    @Transactional(propagation = Propagation.MANDATORY)
    public EmailDeliveryEntity queue(OperatorEntity account, EmailKind kind, AccountTokens.Issued token) {
        return queueTo(account.getId(), account.getEmail(), kind, token);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public EmailDeliveryEntity queueTo(String accountId, String recipient, EmailKind kind, AccountTokens.Issued token) {
        EmailDeliveryEntity delivery = deliveries.save(EmailDeliveryEntity.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .tokenId(token == null ? null : token.entity().getId())
                .kind(kind.name())
                .recipient(recipient)
                .submissionStatus(SubmissionStatus.QUEUED.name())
                .attempts(0)
                .deliveryStatus(DeliveryStatus.UNKNOWN.name())
                .createdAt(Instant.now())
                .build());
        events.publishEvent(new QueuedEmail(delivery.getId(), token == null ? null : token.rawToken()));
        return delivery;
    }

    @Async(SiteSecurityExecutors.ACCOUNT_EMAILS)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(QueuedEmail email) {
        attempt(email.deliveryId(), email.rawToken());
    }

    /**
     * Sends due retries. Emails with a link get a new link (only hashes are stored):
     * the previous link stops working, the expiry stays the original one. Emails
     * left QUEUED or SENDING by a restart are picked up again.
     */
    @Scheduled(fixedDelayString = "${sitesecurity.mail.retry-interval-ms:30000}",
            initialDelayString = "${sitesecurity.mail.retry-initial-delay-ms:30000}")
    public void retryDue() {
        transaction.executeWithoutResult(status -> deliveries.findStuck(Instant.now().minus(STUCK_AFTER)).forEach(stuck -> {
            stuck.setSubmissionStatus(SubmissionStatus.RETRY_SCHEDULED.name());
            stuck.setNextAttemptAt(Instant.now());
            stuck.setLastError("INTERRUPTED");
            deliveries.save(stuck);
        }));
        List<EmailDeliveryEntity> due = deliveries.findDueRetries(Instant.now());
        for (EmailDeliveryEntity delivery : due) {
            try {
                String raw = transaction.execute(status -> reissueLinkFor(delivery.getId()));
                if (raw != null || delivery.getTokenId() == null) {
                    attempt(delivery.getId(), raw);
                }
            } catch (RuntimeException e) {
                log.warn("Retry of account email {} failed: {}", delivery.getId(), e.getClass().getSimpleName());
            }
        }
    }

    /** One attempt. Package-private so that tests can drive it synchronously. */
    void attempt(String deliveryId, String rawToken) {
        Prepared prepared = transaction.execute(status -> prepare(deliveryId, rawToken));
        if (prepared == null) {
            return;
        }
        Result result = sender.send(deliveryId, prepared.recipient(), prepared.subject(), prepared.html());
        transaction.executeWithoutResult(status -> record(deliveryId, result));
    }

    private record Prepared(String recipient, String subject, String html) {
        @Override
        public String toString() {
            return "Prepared[subject=" + subject + "]";
        }
    }

    private Prepared prepare(String deliveryId, String rawToken) {
        EmailDeliveryEntity delivery = deliveries.findById(deliveryId).orElse(null);
        if (delivery == null) {
            return null;
        }
        String state = delivery.getSubmissionStatus();
        if (!state.equals(SubmissionStatus.QUEUED.name()) && !state.equals(SubmissionStatus.RETRY_SCHEDULED.name())) {
            return null;
        }
        EmailKind kind = EmailKind.valueOf(delivery.getKind());
        boolean needsLink = kind == EmailKind.INVITATION || kind == EmailKind.PASSWORD_SETUP || kind == EmailKind.PASSWORD_RESET;
        if (needsLink && (rawToken == null || !links.isConfigured())) {
            markFailed(delivery, links.isConfigured() ? "NO_LINK" : "NO_PUBLIC_URL");
            return null;
        }
        OperatorEntity account = accounts.findById(delivery.getAccountId()).orElse(null);
        String fullName = account == null ? null : account.getFullName();
        String link = switch (kind) {
            case INVITATION, PASSWORD_SETUP -> links.activation(rawToken);
            case PASSWORD_RESET -> links.passwordReset(rawToken);
            case ACTIVATION_NOTICE -> links.isConfigured() ? links.signIn() : null;
            case PASSWORD_CHANGED -> null;
        };
        String extra = null;
        if (kind == EmailKind.ACTIVATION_NOTICE) {
            // Sent to an admin (recipient) about the account that was activated (accountId).
            extra = account == null ? "" : (account.getFullName() != null ? account.getFullName() + " " : "")
                    + "(" + AccountViews.maskEmail(account.getEmail()) + ")";
            fullName = null;
        }
        AccountEmailBuilder.MailContent content = AccountEmailBuilder.build(kind, fullName, link, extra);
        delivery.setSubmissionStatus(SubmissionStatus.SENDING.name());
        delivery.setAttempts(delivery.getAttempts() + 1);
        delivery.setLastAttemptAt(Instant.now());
        delivery.setNextAttemptAt(null);
        deliveries.save(delivery);
        return new Prepared(delivery.getRecipient(), content.subject(), content.html());
    }

    private void record(String deliveryId, Result result) {
        EmailDeliveryEntity delivery = deliveries.findById(deliveryId).orElseThrow();
        delivery.setMessageId(result.messageId());
        delivery.setLastError(result.errorClass());
        switch (result.outcome()) {
            case SUBMITTED -> {
                delivery.setSubmissionStatus(SubmissionStatus.SUBMITTED.name());
                delivery.setSubmittedAt(Instant.now());
                audit.recordSystem(SiteSecurityAuditAction.EMAIL_SUBMITTED, delivery.getAccountId(), delivery.getKind());
            }
            case LOCAL_CONSOLE -> delivery.setSubmissionStatus(SubmissionStatus.LOCAL_CONSOLE.name());
            case MAIL_DISABLED -> {
                delivery.setSubmissionStatus(SubmissionStatus.MAIL_DISABLED.name());
                audit.recordSystem(SiteSecurityAuditAction.EMAIL_SUBMISSION_FAILED, delivery.getAccountId(),
                        delivery.getKind() + " MAIL_DISABLED");
            }
            case TRANSIENT_FAILURE -> {
                int attempts = delivery.getAttempts();
                if (attempts <= RETRY_DELAYS.size()) {
                    delivery.setSubmissionStatus(SubmissionStatus.RETRY_SCHEDULED.name());
                    delivery.setNextAttemptAt(Instant.now().plus(RETRY_DELAYS.get(attempts - 1)));
                } else {
                    markFailedFields(delivery, result.errorClass());
                    audit.recordSystem(SiteSecurityAuditAction.EMAIL_SUBMISSION_FAILED, delivery.getAccountId(),
                            delivery.getKind() + " " + result.errorClass());
                }
            }
            case PERMANENT_FAILURE -> {
                markFailedFields(delivery, result.errorClass());
                audit.recordSystem(SiteSecurityAuditAction.EMAIL_SUBMISSION_FAILED, delivery.getAccountId(),
                        delivery.getKind() + " " + result.errorClass());
            }
        }
        deliveries.save(delivery);
    }

    /** For a retry of an email with a link: a new link if the account still needs it, else the email is dropped. */
    private String reissueLinkFor(String deliveryId) {
        EmailDeliveryEntity delivery = deliveries.findById(deliveryId).orElse(null);
        if (delivery == null || !SubmissionStatus.RETRY_SCHEDULED.name().equals(delivery.getSubmissionStatus())
                || delivery.getTokenId() == null) {
            return null;
        }
        AccountTokenEntity old = tokens.findById(delivery.getTokenId()).orElse(null);
        OperatorEntity account = accounts.findById(delivery.getAccountId()).orElse(null);
        Instant now = Instant.now();
        boolean stillNeeded = old != null && old.isOpenAt(now) && account != null && switch (TokenPurpose.valueOf(old.getPurpose())) {
            case INVITATION -> account.effectiveStatus() == AccountStatus.PENDING
                    || (account.effectiveStatus() == AccountStatus.ACTIVE && !account.hasPassword());
            case PASSWORD_RESET -> account.usableAt(now);
        };
        if (!stillNeeded) {
            markFailed(delivery, "NO_LONGER_NEEDED");
            return null;
        }
        AccountTokens.Issued fresh = accountTokens.replace(old);
        delivery.setTokenId(fresh.entity().getId());
        deliveries.save(delivery);
        return fresh.rawToken();
    }

    private void markFailed(EmailDeliveryEntity delivery, String error) {
        markFailedFields(delivery, error);
        deliveries.save(delivery);
        audit.recordSystem(SiteSecurityAuditAction.EMAIL_SUBMISSION_FAILED, delivery.getAccountId(),
                delivery.getKind() + " " + error);
    }

    private static void markFailedFields(EmailDeliveryEntity delivery, String error) {
        delivery.setSubmissionStatus(SubmissionStatus.FAILED.name());
        delivery.setLastError(error);
        delivery.setNextAttemptAt(null);
    }
}

package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors;
import com.infineonbit.sustainablefarm.modules.sitesecurity.dto.AccountDtos.TokenInfoDto;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.EmailKind;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.TokenPurpose;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.FailedSignInEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.FailedSignInRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService.SignIn;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * What an account does for itself: sign in with a password, open an invitation
 * link, choose or change a password, recover it.
 *
 * <p>Every failure path answers the same way and does the same hashing work, so
 * a caller cannot tell whether an email has an account. Argon2 runs outside any
 * database transaction; the outcome is then recorded in a short transaction that
 * locks the account row (so failure counting cannot be raced). Logs carry account
 * ids only.</p>
 */
@Service
public class AccountAuthService {

    /** Wrong current passwords in "change password" (signed in): pauses changing, never sign-in. */
    static final Duration PASSWORD_FAILURE_WINDOW = Duration.ofMinutes(15);
    static final int PASSWORD_FAILURE_LIMIT = 10;
    static final Duration RESET_MIN_INTERVAL = Duration.ofMinutes(5);
    static final int RESETS_PER_HOUR = 3;
    private static final String NO_ACCOUNT = "no-account";

    private static final Logger log = LoggerFactory.getLogger(AccountAuthService.class);

    /** Any link problem (unknown, used, revoked, expired, wrong purpose, account state): one answer. */
    public static class InvalidLinkException extends RuntimeException {
        public InvalidLinkException() {
            super("This link is invalid or has expired.");
        }
    }

    /** The chosen password breaks the policy; the message is safe to show. */
    public static class PasswordRejectedException extends RuntimeException {
        public PasswordRejectedException(String message) {
            super(message);
        }
    }

    /** Wrong current password or code in a signed-in request. */
    public static class WrongSecretException extends RuntimeException {
        public WrongSecretException(String message) {
            super(message);
        }
    }

    private final OperatorRepository accounts;
    private final FailedSignInRepository failures;
    private final AccountTokens accountTokens;
    private final AccountTokenRepository tokens;
    private final PasswordHasher hasher;
    private final SessionService sessions;
    private final LoginCodeService loginCodes;
    private final AccountEmailService emails;
    private final SiteSecurityAuditService audit;
    private final TransactionTemplate transaction;
    private final SignInThrottle throttle;

    public AccountAuthService(OperatorRepository accounts, FailedSignInRepository failures, AccountTokens accountTokens,
            AccountTokenRepository tokens, PasswordHasher hasher, SessionService sessions, LoginCodeService loginCodes,
            AccountEmailService emails, SiteSecurityAuditService audit, PlatformTransactionManager transactionManager,
            SignInThrottle throttle) {
        this.throttle = throttle;
        this.accounts = accounts;
        this.failures = failures;
        this.accountTokens = accountTokens;
        this.tokens = tokens;
        this.hasher = hasher;
        this.sessions = sessions;
        this.loginCodes = loginCodes;
        this.emails = emails;
        this.audit = audit;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    // ---------------------------------------------------------------- password sign-in

    /** A session, plus the trusted-device cookie value for this browser. */
    public record PasswordSignIn(SignIn signIn, String deviceToken) {
        @Override
        public String toString() {
            return "PasswordSignIn[" + signIn.principal().operatorId() + "]";
        }
    }

    /**
     * Email and password. The sign-in limits ({@link SignInThrottle}) are checked
     * first, before any hashing: a refused attempt is a ThrottledException (429)
     * and costs no Argon2 work. Otherwise exactly one Argon2id verification runs
     * on every path, outside any transaction. There is no account lock: a stranger
     * cannot keep the owner out, because a browser the owner already used keeps its
     * own budget.
     */
    public Optional<PasswordSignIn> login(String email, String password, String presentedSession,
            SignInThrottle.Client client) {
        String normalized = OperatorService.normalizeEmail(email);
        Optional<String> found = accounts.findIdByEmail(normalized);
        SignInThrottle.Verdict verdict = throttle.check(normalized, found, client);
        if (!verdict.allowed()) {
            throw new SignInThrottle.ThrottledException(verdict.retryAfterSeconds());
        }
        String storedHash = found.flatMap(accounts::findPasswordHashById).orElse(null);
        boolean matches = storedHash != null ? hasher.verify(password, storedHash) : dummy(password);
        if (found.isEmpty()) {
            // The same queries as for a real account, so timing says nothing about the email.
            accounts.findPasswordHashById(NO_ACCOUNT);
            throttle.recordFailure(normalized, found, client, null);
            return Optional.empty();
        }
        String accountId = found.get();
        String newHashIfNeeded = matches && hasher.needsRehash(storedHash) ? hasher.hash(password) : null;
        Optional<SignIn> signIn = transaction.execute(status -> {
            OperatorEntity account = accounts.lockById(accountId).orElse(null);
            if (account == null) {
                return Optional.<SignIn>empty();
            }
            Instant now = Instant.now();
            boolean sameHash = Objects.equals(storedHash, account.getPasswordHash());
            boolean usable = account.usableAt(now) && storedHash != null && sameHash;
            if (!usable || !matches) {
                return Optional.<SignIn>empty();
            }
            if (newHashIfNeeded != null) {
                account.setPasswordHash(newHashIfNeeded);
            }
            return Optional.of(sessions.openSession(account, presentedSession, SessionService.Method.PASSWORD));
        });
        if (signIn == null || signIn.isEmpty()) {
            throttle.recordFailure(normalized, found, client, verdict.trustedDeviceId());
            log.info("Wrong password or unusable account {}", accountId);
            return Optional.empty();
        }
        throttle.recordSuccess(normalized, client);
        return Optional.of(new PasswordSignIn(signIn.get(), throttle.trust(accountId, client)));
    }

    private boolean dummy(String password) {
        hasher.verifyAgainstDummy(password);
        return false;
    }

    private void recordPasswordFailure(OperatorEntity account, Instant now) {
        failures.save(FailedSignInEntity.builder()
                .id(UUID.randomUUID().toString())
                .operatorId(account.getId())
                .occurredAt(now)
                .kind(FailedSignInEntity.PASSWORD)
                .build());
        long count = failures.countPasswordFailuresSince(account.getId(), now.minus(PASSWORD_FAILURE_WINDOW));
        log.info("Wrong current password for account {} ({} in the last 15 minutes)", account.getId(), count);
        if (count == PASSWORD_FAILURE_LIMIT) {
            log.warn("Password change paused for 15 minutes for account {} after {} wrong current passwords",
                    account.getId(), PASSWORD_FAILURE_LIMIT);
            audit.recordSystem(SiteSecurityAuditAction.ACCOUNT_SIGN_IN_BLOCKED, account.getId(),
                    "password change paused: " + PASSWORD_FAILURE_LIMIT + " wrong current passwords");
        }
    }

    // ---------------------------------------------------------------- links (invitation, reset)

    /** Who the link is for, without changing anything. */
    @Transactional(readOnly = true)
    public TokenInfoDto inspect(String rawToken) {
        AccountTokenEntity token = accountTokens.find(rawToken).orElseThrow(InvalidLinkException::new);
        OperatorEntity account = usableLinkAccount(token, Instant.now());
        return new TokenInfoDto(AccountValues.api(TokenPurpose.valueOf(token.getPurpose())),
                AccountViews.firstName(account.getFullName()), AccountViews.maskEmail(account.getEmail()),
                token.getExpiresAt());
    }

    /** "Confirm invitation": recorded as activation started. A hint only: scanners can press buttons. */
    @Transactional
    public void confirmInvitation(String rawToken) {
        AccountTokenEntity token = accountTokens.find(rawToken).orElseThrow(InvalidLinkException::new);
        if (!TokenPurpose.INVITATION.name().equals(token.getPurpose())) {
            throw new InvalidLinkException();
        }
        OperatorEntity account = usableLinkAccount(token, Instant.now());
        if (tokens.markConfirmed(token.getId(), Instant.now()) > 0) {
            audit.recordSelf(account.getId(), SiteSecurityAuditAction.INVITATION_CONFIRMED, "link opened");
        }
    }

    /**
     * Completes an invitation (or a password setup link): the only event that makes
     * an account ACTIVE ("accepted"). The password is hashed first, outside the
     * transaction; then the account and the link are locked (in that order) and the
     * link is used with a conditional update, so only one caller can succeed.
     */
    public PasswordSignIn activate(String rawToken, String password, String presentedSession, SignInThrottle.Client client) {
        String accountId = accountTokens.accountIdOf(rawToken).orElseThrow(InvalidLinkException::new);
        checkPolicy(password, accounts.findEmailById(accountId).orElseThrow(InvalidLinkException::new));
        String newHash = hasher.hash(password);
        SignIn signIn = transaction.execute(status -> {
            OperatorEntity account = accounts.lockById(accountId).orElseThrow(InvalidLinkException::new);
            AccountTokenEntity token = accountTokens.lock(rawToken).orElseThrow(InvalidLinkException::new);
            Instant now = Instant.now();
            if (!TokenPurpose.INVITATION.name().equals(token.getPurpose())) {
                throw new InvalidLinkException();
            }
            usableLinkAccount(token, now);
            if (tokens.markUsed(token.getId(), now) == 0) {
                throw new InvalidLinkException();
            }
            tokens.revokeOpen(accountId, TokenPurpose.INVITATION.name(), now);
            boolean wasPending = account.effectiveStatus() == AccountStatus.PENDING;
            account.adoptAccountFields();
            account.setPasswordHash(newHash);
            account.setPasswordSetAt(now);
            if (account.getActivatedAt() == null) {
                account.setActivatedAt(now);
            }
            account.applyStatus(AccountStatus.ACTIVE);
            accounts.save(account);
            if (!wasPending) {
                // A password setup link of a legacy account: its email-code sessions end too.
                sessions.revokeAll(accountId);
            }
            audit.recordSelf(accountId, wasPending ? SiteSecurityAuditAction.ACCOUNT_ACTIVATED
                    : SiteSecurityAuditAction.PASSWORD_SET, wasPending ? "invitation accepted" : "password setup link");
            if (wasPending) {
                notifyAdminsOfActivation(account, now);
            }
            log.info("Account {} {}", accountId, wasPending ? "activated" : "set its password");
            return sessions.openSession(account, presentedSession, SessionService.Method.ACCOUNT_LINK);
        });
        return new PasswordSignIn(signIn, throttle.trust(accountId, client));
    }

    /** Always answers the same. In the background: a reset link for an active account, within limits. */
    @Async(SiteSecurityExecutors.PUBLIC_REQUESTS)
    public void requestPasswordReset(String email) {
        String normalized = OperatorService.normalizeEmail(email);
        transaction.executeWithoutResult(status -> issueReset(normalized));
    }

    private void issueReset(String email) {
        Optional<String> accountId = accounts.findByEmail(email).map(OperatorEntity::getId);
        if (accountId.isEmpty()) {
            return;
        }
        OperatorEntity account = accounts.lockById(accountId.get()).orElse(null);
        Instant now = Instant.now();
        if (account == null || !account.usableAt(now)) {
            return;
        }
        String purpose = TokenPurpose.PASSWORD_RESET.name();
        if (tokens.countByAccountIdAndPurposeAndCreatedAtAfter(account.getId(), purpose, now.minus(RESET_MIN_INTERVAL)) > 0
                || tokens.countByAccountIdAndPurposeAndCreatedAtAfter(account.getId(), purpose, now.minus(Duration.ofHours(1)))
                >= RESETS_PER_HOUR) {
            log.info("Password reset limit reached for account {}", account.getId());
            return;
        }
        // Earlier open reset links stay valid (30 min, at most 3 an hour): a stranger asking
        // for a reset must not be able to cancel the link the owner is about to open.
        AccountTokens.Issued token = accountTokens.issueAlongside(account.getId(), TokenPurpose.PASSWORD_RESET,
                SiteSecurityAuditService.SYSTEM_ACTOR);
        emails.queue(account, EmailKind.PASSWORD_RESET, token);
        audit.recordSystem(SiteSecurityAuditAction.PASSWORD_RESET_REQUESTED, account.getId(), null);
    }

    /**
     * Sets a new password from a reset link. Ends every session and trusted device,
     * lifts every sign-in limit on the email (the owner proved control of the
     * mailbox) and trusts this browser, so the owner can sign in at once even
     * while strangers keep failing. Never re-enables. Returns the device cookie value.
     */
    public String resetPassword(String rawToken, String password, SignInThrottle.Client client) {
        String accountId = accountTokens.accountIdOf(rawToken).orElseThrow(InvalidLinkException::new);
        String email = accounts.findEmailById(accountId).orElseThrow(InvalidLinkException::new);
        checkPolicy(password, email);
        String newHash = hasher.hash(password);
        transaction.executeWithoutResult(status -> {
            OperatorEntity account = accounts.lockById(accountId).orElseThrow(InvalidLinkException::new);
            AccountTokenEntity token = accountTokens.lock(rawToken).orElseThrow(InvalidLinkException::new);
            Instant now = Instant.now();
            if (!TokenPurpose.PASSWORD_RESET.name().equals(token.getPurpose())) {
                throw new InvalidLinkException();
            }
            usableLinkAccount(token, now);
            if (tokens.markUsed(token.getId(), now) == 0) {
                throw new InvalidLinkException();
            }
            tokens.revokeOpen(accountId, TokenPurpose.PASSWORD_RESET.name(), now);
            account.adoptAccountFields();
            account.setPasswordHash(newHash);
            account.setPasswordSetAt(now);
            accounts.save(account);
            failures.deletePasswordFailures(accountId);
            sessions.revokeAll(accountId);
            throttle.forgetDevices(accountId, null);
            throttle.liftAllFor(email);
            audit.recordSelf(accountId, SiteSecurityAuditAction.PASSWORD_RESET, null);
            emails.queue(account, EmailKind.PASSWORD_CHANGED, null);
        });
        return throttle.trust(accountId, client);
    }

    // ---------------------------------------------------------------- signed-in self-service

    /**
     * Change of password while signed in: the current password is required and
     * wrong ones (10 in 15 min) pause changing the password, never sign-in; other
     * sessions and other trusted devices end.
     */
    public void changePassword(String accountId, String currentPassword, String newPassword, String currentSession,
            String currentDevice) {
        String email = accounts.findEmailById(accountId).orElseThrow(() -> new WrongSecretException("Sign in again."));
        String storedHash = accounts.findPasswordHashById(accountId).orElse(null);
        if (storedHash == null) {
            hasher.verifyAgainstDummy(currentPassword);
            throw new WrongSecretException("This account has no password yet.");
        }
        Instant now = Instant.now();
        if (failures.countPasswordFailuresSince(accountId, now.minus(PASSWORD_FAILURE_WINDOW)) >= PASSWORD_FAILURE_LIMIT) {
            hasher.verifyAgainstDummy(currentPassword);
            throw new WrongSecretException("Too many wrong passwords. Try again later.");
        }
        if (!hasher.verify(currentPassword, storedHash)) {
            // Committed on its own: the exception below must not roll the count back.
            transaction.executeWithoutResult(status -> accounts.lockById(accountId)
                    .ifPresent(locked -> recordPasswordFailure(locked, Instant.now())));
            throw new WrongSecretException("The current password is incorrect.");
        }
        checkPolicy(newPassword, email);
        String newHash = hasher.hash(newPassword);
        transaction.executeWithoutResult(status -> {
            OperatorEntity locked = accounts.lockById(accountId).orElseThrow(() -> new WrongSecretException("Sign in again."));
            if (!Objects.equals(storedHash, locked.getPasswordHash())) {
                throw new WrongSecretException("The password was changed meanwhile. Sign in again.");
            }
            locked.setPasswordHash(newHash);
            locked.setPasswordSetAt(Instant.now());
            accounts.save(locked);
            sessions.revokeOthers(accountId, currentSession);
            throttle.forgetDevices(accountId, currentDevice);
            audit.recordSelf(accountId, SiteSecurityAuditAction.PASSWORD_CHANGED, null);
            emails.queue(locked, EmailKind.PASSWORD_CHANGED, null);
        });
    }

    /**
     * Migration stage 1: an account signed in with an email code sets its first
     * password. It must prove itself again with a fresh email code (a code is
     * valid 5 minutes, within the required 10). Afterwards it signs in with the
     * password only.
     */
    public void setupPassword(String accountId, String code, String password, String currentSession) {
        String email = accounts.findEmailById(accountId).orElseThrow(() -> new WrongSecretException("Sign in again."));
        if (accounts.findPasswordHashById(accountId).isPresent()) {
            throw new WrongSecretException("This account already has a password. Use \"Change password\".");
        }
        checkPolicy(password, email);
        // Own transaction, committed before any error is returned: a wrong code always
        // counts against the code's five attempts.
        Optional<String> verified = loginCodes.verifyCode(email, code);
        if (verified.isEmpty() || !verified.get().equals(accountId)) {
            throw new WrongSecretException("This code is invalid or has expired.");
        }
        String newHash = hasher.hash(password);
        transaction.executeWithoutResult(status -> {
            OperatorEntity locked = accounts.lockById(accountId).orElseThrow();
            if (locked.hasPassword()) {
                throw new WrongSecretException("This account already has a password. Use \"Change password\".");
            }
            Instant now = Instant.now();
            locked.adoptAccountFields();
            locked.setPasswordHash(newHash);
            locked.setPasswordSetAt(now);
            if (locked.getActivatedAt() == null) {
                locked.setActivatedAt(now);
            }
            accounts.save(locked);
            tokens.revokeOpen(accountId, TokenPurpose.INVITATION.name(), now);
            sessions.revokeOthers(accountId, currentSession);
            audit.recordSelf(accountId, SiteSecurityAuditAction.PASSWORD_SET, "email code + session");
        });
    }

    // ---------------------------------------------------------------- helpers

    /** The account a link may act on, or an InvalidLinkException (one answer for every reason). */
    private OperatorEntity usableLinkAccount(AccountTokenEntity token, Instant now) {
        if (!token.isOpenAt(now)) {
            throw new InvalidLinkException();
        }
        OperatorEntity account = accounts.findById(token.getAccountId()).orElseThrow(InvalidLinkException::new);
        boolean ok = switch (TokenPurpose.valueOf(token.getPurpose())) {
            case INVITATION -> !account.validityEndedAt(now)
                    && (account.effectiveStatus() == AccountStatus.PENDING
                    || (account.effectiveStatus() == AccountStatus.ACTIVE && !account.hasPassword()));
            case PASSWORD_RESET -> account.usableAt(now);
        };
        if (!ok) {
            throw new InvalidLinkException();
        }
        return account;
    }

    private static void checkPolicy(String password, String email) {
        PasswordPolicy.problem(password, email).ifPresent(problem -> {
            throw new PasswordRejectedException(problem);
        });
    }

    private void notifyAdminsOfActivation(OperatorEntity activated, Instant now) {
        accounts.findAll().stream()
                .filter(admin -> admin.effectiveRole() == AccountRole.ADMIN && admin.usableAt(now))
                .filter(admin -> !admin.getId().equals(activated.getId()))
                .forEach(admin -> emails.queueTo(activated.getId(), admin.getEmail(), EmailKind.ACTIVATION_NOTICE, null));
    }
}

package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorSessionEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account sessions. The browser keeps a random value in an HttpOnly cookie, the
 * database keeps only its hash. A session lasts 8 hours from sign-in (less if
 * the account's validity ends sooner) and is checked against the account on
 * every request: status, validity, role, permissions and zones.
 */
@Service
public class SessionService {

    public static final String COOKIE_NAME = "sitesecurity_session";
    public static final String COOKIE_PATH = "/api/v1/sitesecurity";
    static final Duration SESSION_DURATION = Duration.ofHours(8);

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);
    private static final int TOKEN_BYTES = 32;

    private final OperatorRepository operatorRepository;
    private final OperatorSessionRepository sessionRepository;
    private final LoginCodeService loginCodeService;
    private final CodeSignInSettings codeSignInSettings;
    private final SiteSecurityAuditService audit;
    private final boolean secureCookie;
    private final SecureRandom random = new SecureRandom();

    public SessionService(OperatorRepository operatorRepository, OperatorSessionRepository sessionRepository,
            LoginCodeService loginCodeService, CodeSignInSettings codeSignInSettings, SiteSecurityAuditService audit,
            @Value("${SITESECURITY_COOKIE_SECURE:true}") boolean secureCookie) {
        this.operatorRepository = operatorRepository;
        this.sessionRepository = sessionRepository;
        this.loginCodeService = loginCodeService;
        this.codeSignInSettings = codeSignInSettings;
        this.audit = audit;
        this.secureCookie = secureCookie;
        if (!secureCookie) {
            log.warn("SITESECURITY_COOKIE_SECURE is false: the session cookie can travel over plain HTTP.");
        }
    }

    /** A new session and the cookie value that opens it. The value is never stored. */
    public record SignIn(String token, SiteSecurityPrincipal principal, Instant expiresAt) {
        @Override
        public String toString() {
            return "SignIn[principal=" + principal + "]";
        }
    }

    /**
     * Legacy email-code sign-in (migration stages 0 to 2): only for an active
     * account that has no password yet, and only until the method is retired.
     */
    @Transactional
    public Optional<SignIn> signInWithCode(String email, String code, String presentedToken) {
        if (codeSignInSettings.isRetired()) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        Optional<OperatorEntity> account = loginCodeService.verifyCode(email, code)
                .flatMap(operatorRepository::findById)
                .filter(found -> found.usableAt(now))
                .filter(found -> !found.hasPassword());
        return account.map(found -> openSession(found, presentedToken, Method.EMAIL_CODE));
    }

    /** Kept for callers and tests of the code sign-in. */
    @Transactional
    public Optional<SignIn> signIn(String email, String code) {
        return signInWithCode(email, code, null);
    }

    /** How the account proved who it is. */
    public enum Method {
        PASSWORD("password"), EMAIL_CODE("email code"), ACCOUNT_LINK("activation link");

        final String label;

        Method(String label) {
            this.label = label;
        }
    }

    /**
     * Opens a session for an account that has just proved who it is. Any session
     * the browser already carries is closed first (no session fixation).
     */
    @Transactional
    public SignIn openSession(OperatorEntity account, String presentedToken, Method method) {
        Instant now = Instant.now();
        if (presentedToken != null && !presentedToken.isBlank()) {
            sessionRepository.revokeByTokenHash(hash(presentedToken), now);
        }
        sessionRepository.deleteFinished(account.getId(), now);
        Instant expiresAt = now.plus(SESSION_DURATION);
        Instant validityEnd = account.validityEnd();
        if (validityEnd != null && validityEnd.isBefore(expiresAt)) {
            expiresAt = validityEnd;
        }
        String token = newToken();
        sessionRepository.save(OperatorSessionEntity.builder()
                .id(UUID.randomUUID().toString())
                .operatorId(account.getId())
                .tokenHash(hash(token))
                .createdAt(now)
                .expiresAt(expiresAt)
                .build());
        account.setLastLoginAt(now);
        if (method == Method.PASSWORD) {
            // Only a real password sign-in counts toward the stage 3 condition; finishing a link does not.
            account.setLastPasswordLoginAt(now);
        }
        if (account.getFirstLoginAt() == null) {
            account.setFirstLoginAt(now);
            audit.recordSelf(account.getId(), SiteSecurityAuditAction.ACCOUNT_FIRST_SIGN_IN,
                    method.label);
        }
        operatorRepository.save(account);
        log.info("Account {} signed in ({})", account.getId(), method.label);
        return new SignIn(token, principalOf(account), expiresAt);
    }

    /** The caller behind a cookie value, if the session is open and the account still usable. */
    @Transactional(readOnly = true)
    public Optional<SiteSecurityPrincipal> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        return sessionRepository.findByTokenHash(hash(token))
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> session.getExpiresAt().isAfter(now))
                .flatMap(session -> operatorRepository.findById(session.getOperatorId()))
                .filter(account -> account.usableAt(now))
                .map(SessionService::principalOf);
    }

    /** The account behind a cookie value (for account self-service), if usable. */
    @Transactional(readOnly = true)
    public Optional<String> accountIdOf(String token) {
        return resolve(token).map(SiteSecurityPrincipal::operatorId);
    }

    @Transactional
    public void signOut(String token) {
        if (token != null && !token.isBlank()
                && sessionRepository.revokeByTokenHash(hash(token), Instant.now()) > 0) {
            log.info("A Site Security session was closed");
        }
    }

    /** Ends every open session of an account (disable, password reset). */
    @Transactional
    public void revokeAll(String operatorId) {
        int revoked = sessionRepository.revokeAllForOperator(operatorId, Instant.now());
        if (revoked > 0) {
            log.info("Closed {} session(s) of account {}", revoked, operatorId);
        }
    }

    /** Ends every other session of the account (password change): the current one stays open. */
    @Transactional
    public void revokeOthers(String operatorId, String keepToken) {
        if (keepToken == null || keepToken.isBlank()) {
            revokeAll(operatorId);
            return;
        }
        sessionRepository.revokeOthersForOperator(operatorId, hash(keepToken), Instant.now());
    }

    public ResponseCookie sessionCookie(SignIn signIn) {
        long millis = Duration.between(Instant.now(), signIn.expiresAt()).toMillis();
        // Whole seconds, rounded up, so that a fresh 8-hour session announces exactly 28800 s.
        long seconds = millis <= 0 ? 0 : (millis + 999) / 1000;
        return cookie(signIn.token(), Duration.ofSeconds(Math.min(seconds, SESSION_DURATION.toSeconds())));
    }

    public ResponseCookie clearedCookie() {
        return cookie("", Duration.ZERO);
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(COOKIE_PATH)
                .maxAge(maxAge)
                .build();
    }

    public static SiteSecurityPrincipal principalOf(OperatorEntity account) {
        SiteSecurityRole role = account.effectiveRole() == AccountRole.ADMIN ? SiteSecurityRole.ADMIN : SiteSecurityRole.USER;
        return new SiteSecurityPrincipal(account.getId(), account.getEmail(), role,
                account.effectivePermissions(), account.effectiveZoneIds(), account.getFullName(), account.hasPassword());
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of a cookie or link value, as stored in the database. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorSessionEntity;
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
 * Operator sessions. The browser keeps a random value in an HttpOnly cookie,
 * the database keeps only its hash. A session lasts 8 hours from sign-in and is
 * checked against the operator's row on every request.
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
    private final boolean secureCookie;
    private final SecureRandom random = new SecureRandom();

    public SessionService(OperatorRepository operatorRepository, OperatorSessionRepository sessionRepository,
            LoginCodeService loginCodeService, @Value("${SITESECURITY_COOKIE_SECURE:true}") boolean secureCookie) {
        this.operatorRepository = operatorRepository;
        this.sessionRepository = sessionRepository;
        this.loginCodeService = loginCodeService;
        this.secureCookie = secureCookie;
        if (!secureCookie) {
            log.warn("SITESECURITY_COOKIE_SECURE is false: the session cookie can travel over plain HTTP.");
        }
    }

    /** A new session and the cookie value that opens it. The value is never stored. */
    public record SignIn(String token, SiteSecurityPrincipal principal) {
        @Override
        public String toString() {
            return "SignIn[principal=" + principal + "]";
        }
    }

    /** Checks the code and, when it is right, opens a session in the same transaction. */
    @Transactional
    public Optional<SignIn> signIn(String email, String code) {
        Optional<OperatorEntity> operator = loginCodeService.verifyCode(email, code)
                .flatMap(operatorRepository::findById)
                .filter(OperatorEntity::isActive);
        if (operator.isEmpty()) {
            return Optional.empty();
        }
        OperatorEntity signedIn = operator.get();
        Instant now = Instant.now();
        sessionRepository.deleteFinished(signedIn.getId(), now);
        String token = newToken();
        sessionRepository.save(OperatorSessionEntity.builder()
                .id(UUID.randomUUID().toString())
                .operatorId(signedIn.getId())
                .tokenHash(hash(token))
                .createdAt(now)
                .expiresAt(now.plus(SESSION_DURATION))
                .build());
        signedIn.setLastLoginAt(now);
        log.info("Operator {} signed in", signedIn.getId());
        return Optional.of(new SignIn(token, principalOf(signedIn)));
    }

    /** The caller behind a cookie value, if the session is still open and the operator still active. */
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
                .filter(OperatorEntity::isActive)
                .map(SessionService::principalOf);
    }

    @Transactional
    public void signOut(String token) {
        if (token != null && !token.isBlank()
                && sessionRepository.revokeByTokenHash(hash(token), Instant.now()) > 0) {
            log.info("A Site Security session was closed");
        }
    }

    /** Ends every open session of an operator, for example when the operator is disabled. */
    @Transactional
    public void revokeAll(String operatorId) {
        int revoked = sessionRepository.revokeAllForOperator(operatorId, Instant.now());
        if (revoked > 0) {
            log.info("Closed {} session(s) of operator {}", revoked, operatorId);
        }
    }

    public ResponseCookie sessionCookie(String token) {
        return cookie(token, SESSION_DURATION);
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

    private static SiteSecurityPrincipal principalOf(OperatorEntity operator) {
        return new SiteSecurityPrincipal(operator.getId(), operator.getEmail(), operator.getRole());
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of a cookie value, as stored in the database. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

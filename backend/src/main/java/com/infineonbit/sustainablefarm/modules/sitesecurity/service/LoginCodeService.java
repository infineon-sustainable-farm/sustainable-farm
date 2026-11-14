package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.FailedSignInEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LoginCodeEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.FailedSignInRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Issues and checks the one-time codes operators sign in with.
 *
 * <p>Requesting a code runs in the background, so a caller cannot tell from the
 * answer or its timing whether an email is allowed, or whether a limit applied.
 * Codes are stored as an HMAC: six digits alone could be guessed from a leaked
 * database. Logs carry the operator id, never the email or the code.</p>
 */
@Service
public class LoginCodeService {

    static final Duration CODE_VALIDITY = Duration.ofMinutes(5);
    static final int MAX_ATTEMPTS = 5;
    static final Duration MIN_DELAY_BETWEEN_CODES = Duration.ofMinutes(1);
    static final Duration RATE_WINDOW = Duration.ofHours(1);
    static final int MAX_CODES_PER_WINDOW = 5;
    /**
     * Adaptive codes: once an operator has this many wrong codes in the last
     * 24 hours, new codes have 10 digits instead of 6, until the count falls
     * back below the threshold. Nobody is locked out: guessing simply becomes
     * hopeless (25 guesses an hour against 10^10 codes).
     */
    static final Duration ADAPTIVE_WINDOW = Duration.ofHours(24);
    static final int ADAPTIVE_THRESHOLD = 10;
    static final int SHORT_CODE_DIGITS = 6;
    static final int LONG_CODE_DIGITS = 10;

    private static final Logger log = LoggerFactory.getLogger(LoginCodeService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;

    private final OperatorRepository operatorRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final FailedSignInRepository failedSignInRepository;
    private final NotificationService notificationService;
    private final ObjectProvider<CodeSignInSettings> codeSignInSettings;
    private final TransactionTemplate transaction;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec hmacKey;
    private final boolean canSendCodes;

    public LoginCodeService(
            OperatorRepository operatorRepository,
            LoginCodeRepository loginCodeRepository,
            FailedSignInRepository failedSignInRepository,
            NotificationService notificationService,
            ObjectProvider<CodeSignInSettings> codeSignInSettings,
            PlatformTransactionManager transactionManager,
            Environment environment,
            @Value("${SITESECURITY_OTP_SECRET:}") String secret,
            @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.operatorRepository = operatorRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.failedSignInRepository = failedSignInRepository;
        this.notificationService = notificationService;
        this.codeSignInSettings = codeSignInSettings;
        this.transaction = new TransactionTemplate(transactionManager);
        this.hmacKey = configuredKey(secret);
        // Without SMTP, the console sink would write the code to the logs: only acceptable in dev.
        boolean mailIsSafe = mailEnabled || environment.acceptsProfiles(Profiles.of("dev"));
        if (!mailIsSafe) {
            log.warn("MAIL_ENABLED is false: sign-in codes will not be sent.");
        }
        this.canSendCodes = hmacKey != null && mailIsSafe;
    }

    /** Sends a code if this email belongs to an active operator within the limits. */
    @Async(SiteSecurityExecutors.PUBLIC_REQUESTS)
    public void requestCode(String email) {
        if (!canSendCodes) {
            return;
        }
        String normalizedEmail = OperatorService.normalizeEmail(email);
        IssuedCode issued = transaction.execute(status -> issueCode(normalizedEmail));
        if (issued != null) {
            sendCode(normalizedEmail, issued);
        }
    }

    /**
     * Returns the id of the operator the code belongs to, or nothing. Every failure
     * gives the same result: the caller never learns which check failed.
     */
    @Transactional
    public Optional<String> verifyCode(String email, String code) {
        if (hmacKey == null) {
            return Optional.empty();
        }
        List<LoginCodeEntity> candidates = loginCodeRepository
                .lockUnusedByActiveOperatorEmail(OperatorService.normalizeEmail(email));
        Instant now = Instant.now();
        Optional<LoginCodeEntity> usable = candidates.stream()
                .filter(candidate -> candidate.getExpiresAt().isAfter(now))
                .filter(candidate -> candidate.getAttempts() < MAX_ATTEMPTS)
                .findFirst();
        if (usable.isEmpty()) {
            return Optional.empty();
        }
        LoginCodeEntity loginCode = usable.get();
        loginCode.setAttempts(loginCode.getAttempts() + 1);
        boolean matches = MessageDigest.isEqual(
                bytes(hash(loginCode.getId(), code)), bytes(loginCode.getCodeHash()));
        if (!matches) {
            // Saved in this transaction, with the code row locked: parallel guesses are all counted.
            failedSignInRepository.save(FailedSignInEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .operatorId(loginCode.getOperatorId())
                    .occurredAt(now)
                    .kind(FailedSignInEntity.CODE)
                    .build());
            log.info("Wrong sign-in code for operator {} (attempt {} of {})",
                    loginCode.getOperatorId(), loginCode.getAttempts(), MAX_ATTEMPTS);
            return Optional.empty();
        }
        loginCode.setConsumedAt(now);
        return Optional.of(loginCode.getOperatorId());
    }

    private IssuedCode issueCode(String email) {
        Optional<OperatorEntity> operator = operatorRepository.lockActiveByEmail(email);
        if (operator.isEmpty()) {
            return null;
        }
        // Email codes are the legacy sign-in: never for an account that has a password,
        // and not at all once the method is retired.
        if (operator.get().hasPassword() || isRetired() || !operator.get().usableAt(Instant.now())) {
            log.info("No sign-in code for operator {}: it signs in with a password or codes are retired",
                    operator.get().getId());
            return null;
        }
        String operatorId = operator.get().getId();
        Instant now = Instant.now();
        loginCodeRepository.deleteOlderThan(operatorId, now.minus(RATE_WINDOW));
        // Anyone who knows the email can ask for a code. While the last code can
        // still be used, a new request must not replace it, nor use up the hourly
        // limit: the operator already has that code in their inbox.
        if (hasUsableCode(operatorId, now)) {
            log.info("A sign-in code is still valid for operator {}: no new code sent", operatorId);
            return null;
        }
        if (isRateLimited(operatorId, now)) {
            log.info("Sign-in code limit reached for operator {}", operatorId);
            return null;
        }
        // Only expired or blocked codes are left: close them before issuing a new one.
        loginCodeRepository.consumeUnused(operatorId, now);

        int digits = codeLength(operatorId, now);
        String id = UUID.randomUUID().toString();
        String code = formatCode(random.nextLong(powerOfTen(digits)), digits);
        loginCodeRepository.save(LoginCodeEntity.builder()
                .id(id)
                .operatorId(operatorId)
                .codeHash(hash(id, code))
                .expiresAt(now.plus(CODE_VALIDITY))
                .createdAt(now)
                .build());
        return new IssuedCode(operatorId, code);
    }

    /** 10 digits while the operator has had at least 10 wrong codes in the last 24 hours, else 6. */
    private int codeLength(String operatorId, Instant now) {
        Instant since = now.minus(ADAPTIVE_WINDOW);
        failedSignInRepository.deleteOlderThan(operatorId, since);
        long wrongCodes = failedSignInRepository.countCodeFailuresSince(operatorId, since);
        if (wrongCodes >= ADAPTIVE_THRESHOLD) {
            log.info("Operator {} has {} wrong sign-in codes in 24 hours: sending a {}-digit code",
                    operatorId, wrongCodes, LONG_CODE_DIGITS);
            return LONG_CODE_DIGITS;
        }
        return SHORT_CODE_DIGITS;
    }

    /** Keeps the leading zeros: 42 with 10 digits is "0000000042". */
    static String formatCode(long value, int digits) {
        // Locale.ROOT: ASCII digits whatever the server's locale, in the email and in the hash.
        return String.format(Locale.ROOT, "%0" + digits + "d", value);
    }

    private static long powerOfTen(int digits) {
        long result = 1;
        for (int i = 0; i < digits; i++) {
            result *= 10;
        }
        return result;
    }

    private boolean isRetired() {
        CodeSignInSettings settings = codeSignInSettings.getIfAvailable();
        return settings != null && settings.isRetired();
    }

    /** Called with the operator row locked, so concurrent requests see the same answer. */
    private boolean hasUsableCode(String operatorId, Instant now) {
        return loginCodeRepository.findByOperatorIdAndConsumedAtIsNull(operatorId).stream()
                .anyMatch(candidate -> candidate.getExpiresAt().isAfter(now)
                        && candidate.getAttempts() < MAX_ATTEMPTS);
    }

    private boolean isRateLimited(String operatorId, Instant now) {
        return loginCodeRepository.countByOperatorIdAndCreatedAtAfter(
                        operatorId, now.minus(MIN_DELAY_BETWEEN_CODES)) > 0
                || loginCodeRepository.countByOperatorIdAndCreatedAtAfter(
                        operatorId, now.minus(RATE_WINDOW)) >= MAX_CODES_PER_WINDOW;
    }

    /** Sent outside the transaction, so that a slow mail server holds no database lock. */
    private void sendCode(String email, IssuedCode issued) {
        boolean sent = false;
        try {
            sent = notificationService.send(email, "Votre code de connexion", codeEmail(issued.code()));
        } catch (RuntimeException e) {
            log.warn("Sign-in code email error for operator {}: {}",
                    issued.operatorId(), e.getClass().getSimpleName());
        }
        if (!sent) {
            log.warn("Sign-in code was not sent to operator {}", issued.operatorId());
        }
    }

    private static String codeEmail(String code) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:560px;margin:auto;\">"
                + "<h2>Votre code de connexion</h2>"
                + "<p style=\"font-size:24px;letter-spacing:4px;\"><strong>" + code + "</strong></p>"
                + "<p>Ce code de " + code.length() + " chiffres est valable " + CODE_VALIDITY.toMinutes()
                + " minutes et ne peut servir qu'une fois.</p>"
                + "<p>Si vous n'avez pas demandé ce code, ignorez cet email.</p></div>";
    }

    /** Tied to the code's id, so that the same digits never produce the same hash twice. Package-private for tests. */
    String hash(String id, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(bytes(id + ":" + code)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static SecretKeySpec configuredKey(String secret) {
        if (secret == null || secret.trim().length() < MIN_SECRET_LENGTH) {
            log.warn("SITESECURITY_OTP_SECRET is missing or shorter than {} characters: "
                    + "sign-in codes are disabled.", MIN_SECRET_LENGTH);
            return null;
        }
        return new SecretKeySpec(bytes(secret.trim()), HMAC_ALGORITHM);
    }

    private record IssuedCode(String operatorId, String code) {
        @Override
        public String toString() {
            return "IssuedCode[operatorId=" + operatorId + "]";
        }
    }
}

package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LoginCodeEntity;
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
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(LoginCodeService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MIN_SECRET_LENGTH = 32;
    private static final int CODE_RANGE = 1_000_000;

    private final OperatorRepository operatorRepository;
    private final LoginCodeRepository loginCodeRepository;
    private final NotificationService notificationService;
    private final TransactionTemplate transaction;
    private final SecureRandom random = new SecureRandom();
    private final SecretKeySpec hmacKey;
    private final boolean canSendCodes;

    public LoginCodeService(
            OperatorRepository operatorRepository,
            LoginCodeRepository loginCodeRepository,
            NotificationService notificationService,
            PlatformTransactionManager transactionManager,
            Environment environment,
            @Value("${SITESECURITY_OTP_SECRET:}") String secret,
            @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.operatorRepository = operatorRepository;
        this.loginCodeRepository = loginCodeRepository;
        this.notificationService = notificationService;
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
    @Async
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

    /** Every failure gives the same result: the caller never learns which check failed. */
    @Transactional
    public boolean verifyCode(String email, String code) {
        if (hmacKey == null) {
            return false;
        }
        List<LoginCodeEntity> candidates = loginCodeRepository
                .lockUnusedByActiveOperatorEmail(OperatorService.normalizeEmail(email));
        Instant now = Instant.now();
        Optional<LoginCodeEntity> usable = candidates.stream()
                .filter(candidate -> candidate.getExpiresAt().isAfter(now))
                .filter(candidate -> candidate.getAttempts() < MAX_ATTEMPTS)
                .findFirst();
        if (usable.isEmpty()) {
            return false;
        }
        LoginCodeEntity loginCode = usable.get();
        loginCode.setAttempts(loginCode.getAttempts() + 1);
        boolean matches = MessageDigest.isEqual(
                bytes(hash(loginCode.getId(), code)), bytes(loginCode.getCodeHash()));
        if (matches) {
            loginCode.setConsumedAt(now);
        }
        return matches;
    }

    private IssuedCode issueCode(String email) {
        Optional<OperatorEntity> operator = operatorRepository.lockActiveByEmail(email);
        if (operator.isEmpty()) {
            return null;
        }
        String operatorId = operator.get().getId();
        Instant now = Instant.now();
        loginCodeRepository.deleteOlderThan(operatorId, now.minus(RATE_WINDOW));
        if (isRateLimited(operatorId, now)) {
            log.info("Sign-in code limit reached for operator {}", operatorId);
            return null;
        }
        loginCodeRepository.consumeUnused(operatorId, now);

        String id = UUID.randomUUID().toString();
        String code = "%06d".formatted(random.nextInt(CODE_RANGE));
        loginCodeRepository.save(LoginCodeEntity.builder()
                .id(id)
                .operatorId(operatorId)
                .codeHash(hash(id, code))
                .expiresAt(now.plus(CODE_VALIDITY))
                .createdAt(now)
                .build());
        return new IssuedCode(operatorId, code);
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
                + "<p>Ce code est valable " + CODE_VALIDITY.toMinutes() + " minutes et ne peut servir qu'une fois.</p>"
                + "<p>Si vous n'avez pas demandé ce code, ignorez cet email.</p></div>";
    }

    /** Tied to the code's id, so that the same digits never produce the same hash twice. */
    private String hash(String id, String code) {
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

package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SignInFailureEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.TrustedDeviceEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SignInFailureRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.TrustedDeviceRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limits on password sign-in that stop guessing without letting a stranger lock
 * the owner out. There is no account lock. Every check runs before any Argon2
 * work, so a refused attempt costs no hashing; refusals are a 429 with
 * Retry-After. Every key is the same whether or not the email has an account.
 *
 * <ul>
 *   <li><b>Per client address and email</b>: 3 free failures, then a progressive
 *   delay before the next try (30 s, 1, 2, 4, 8, then 15 min).</li>
 *   <li><b>Per client address</b> (any email): 30 failures in 15 min, then that
 *   address waits (stops password spraying from one address).</li>
 *   <li><b>Per email, all addresses together</b>: 20 failures in 15 min, then
 *   attempts from new browsers wait (stops distributed guessing).</li>
 *   <li><b>Trusted device</b>: a browser that already proved it belongs to the
 *   account (password sign-in, activation, reset) skips the three limits above
 *   and has its own budget of 5 failures in 15 min. An attacker without that
 *   browser therefore cannot keep the owner out; a stolen device cookie gives
 *   5 guesses per 15 min and nothing else.</li>
 * </ul>
 *
 * <p>An owner on a new browser while their email is under a distributed attack
 * has the password reset: reset links can no longer be cancelled by strangers,
 * a reset lifts every limit on the email and trusts the browser it was done in.</p>
 */
@Service
public class SignInThrottle {

    public static final String DEVICE_COOKIE = "sitesecurity_device";
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final int FREE_FAILURES_PER_CLIENT_AND_EMAIL = 3;
    static final Duration FIRST_DELAY = Duration.ofSeconds(30);
    static final Duration MAX_DELAY = Duration.ofMinutes(15);
    static final int CLIENT_FAILURE_LIMIT = 30;
    static final int EMAIL_FAILURE_LIMIT = 20;
    static final int DEVICE_FAILURE_LIMIT = 5;
    static final Duration DEVICE_VALIDITY = Duration.ofDays(90);
    static final int MAX_DEVICES_PER_ACCOUNT = 10;
    private static final int TOKEN_BYTES = 32;

    private static final Logger log = LoggerFactory.getLogger(SignInThrottle.class);

    /** Where an attempt comes from: the client address and the device cookie it carries, if any. */
    public record Client(String address, String deviceToken) {
        @Override
        public String toString() {
            return "Client[hidden]";
        }
    }

    /** Allowed (possibly through a trusted device), or refused for this many seconds. */
    public record Verdict(boolean allowed, long retryAfterSeconds, String trustedDeviceId) {
        static Verdict allow(String deviceId) {
            return new Verdict(true, 0, deviceId);
        }

        static Verdict waitFor(Duration duration) {
            return new Verdict(false, Math.max(1, (duration.toMillis() + 999) / 1000), null);
        }
    }

    /** Thrown to the controller: answered 429 with Retry-After, before any password work. */
    public static class ThrottledException extends RuntimeException {
        private final long retryAfterSeconds;

        public ThrottledException(long retryAfterSeconds) {
            super("Too many sign-in attempts. Wait a few minutes, use a browser you signed in with before, or reset your password.");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }

    private final SignInFailureRepository failures;
    private final TrustedDeviceRepository devices;
    private final SiteSecurityAuditService audit;
    private final boolean secureCookie;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SignInThrottle(SignInFailureRepository failures, TrustedDeviceRepository devices, SiteSecurityAuditService audit,
            @Value("${SITESECURITY_COOKIE_SECURE:true}") boolean secureCookie) {
        this.failures = failures;
        this.devices = devices;
        this.audit = audit;
        this.secureCookie = secureCookie;
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    // ---------------------------------------------------------------- limits

    @Transactional(readOnly = true)
    public Verdict check(String email, Optional<String> accountId, Client client) {
        Instant now = Instant.now();
        Instant since = now.minus(WINDOW);
        Optional<TrustedDeviceEntity> device = trustedDevice(accountId, client, now);
        if (device.isPresent() && failures.countByDeviceIdAndOccurredAtAfter(device.get().getId(), since) < DEVICE_FAILURE_LIMIT) {
            return Verdict.allow(device.get().getId());
        }
        String emailKey = emailKey(email);
        String clientKey = clientKey(client);
        if (failures.countByClientKeyAndOccurredAtAfter(clientKey, since) >= CLIENT_FAILURE_LIMIT) {
            return Verdict.waitFor(untilWindowFrees(failures.firstForClient(clientKey, since), now));
        }
        if (failures.countByEmailKeyAndOccurredAtAfter(emailKey, since) >= EMAIL_FAILURE_LIMIT) {
            return Verdict.waitFor(untilWindowFrees(failures.firstForEmail(emailKey, since), now));
        }
        long pair = failures.countByEmailKeyAndClientKeyAndOccurredAtAfter(emailKey, clientKey, since);
        if (pair >= FREE_FAILURES_PER_CLIENT_AND_EMAIL) {
            Duration delay = delayAfter(pair);
            Instant last = failures.lastForEmailAndClient(emailKey, clientKey, since).orElse(now.minus(delay));
            Instant next = last.plus(delay);
            if (next.isAfter(now)) {
                return Verdict.waitFor(Duration.between(now, next));
            }
        }
        return Verdict.allow(null);
    }

    /** 30 s after the 3rd failure, doubling for each further one, at most 15 min. */
    static Duration delayAfter(long failuresSoFar) {
        long steps = Math.max(0, failuresSoFar - FREE_FAILURES_PER_CLIENT_AND_EMAIL);
        if (steps >= 5) {
            return MAX_DELAY;
        }
        Duration delay = FIRST_DELAY.multipliedBy(1L << steps);
        return delay.compareTo(MAX_DELAY) > 0 ? MAX_DELAY : delay;
    }

    private static Duration untilWindowFrees(Optional<Instant> oldest, Instant now) {
        return oldest.map(o -> Duration.between(now, o.plus(WINDOW))).filter(d -> !d.isNegative()).orElse(Duration.ofSeconds(1));
    }

    /** The same row for every failure (unknown email, wrong password, unusable account). */
    @Transactional
    public void recordFailure(String email, Optional<String> accountId, Client client, String trustedDeviceId) {
        Instant now = Instant.now();
        String emailKey = emailKey(email);
        failures.save(SignInFailureEntity.builder()
                .id(UUID.randomUUID().toString())
                .emailKey(emailKey)
                .clientKey(clientKey(client))
                .deviceId(trustedDeviceId)
                .occurredAt(now)
                .build());
        if (accountId.isPresent() && trustedDeviceId == null
                && failures.countByEmailKeyAndOccurredAtAfter(emailKey, now.minus(WINDOW)) == EMAIL_FAILURE_LIMIT) {
            long addresses = failures.countClientsForEmailSince(emailKey, now.minus(WINDOW));
            log.warn("Password sign-in from new browsers paused for account {}: {} failures in 15 min from {} addresses",
                    accountId.get(), EMAIL_FAILURE_LIMIT, addresses);
            audit.recordSystem(SiteSecurityAuditAction.ACCOUNT_SIGN_IN_BLOCKED, accountId.get(),
                    "new browsers paused: " + EMAIL_FAILURE_LIMIT + " failures/15 min, " + addresses + " addresses");
        }
    }

    /** A successful sign-in clears this client's delay for this email (not other clients' failures). */
    @Transactional
    public void recordSuccess(String email, Client client) {
        failures.deleteForEmailAndClient(emailKey(email), clientKey(client));
    }

    /** A successful password reset: the owner proved control of the mailbox. */
    @Transactional
    public void liftAllFor(String email) {
        failures.deleteForEmail(emailKey(email));
    }

    // ---------------------------------------------------------------- trusted devices

    /**
     * Trusts the browser for the account and returns the raw cookie value. A
     * browser that already carries a valid device of this account keeps it.
     */
    @Transactional
    public String trust(String accountId, Client client) {
        Instant now = Instant.now();
        Optional<TrustedDeviceEntity> existing = trustedDevice(Optional.of(accountId), client, now);
        if (existing.isPresent()) {
            existing.get().setLastUsedAt(now);
            devices.save(existing.get());
            return client.deviceToken();
        }
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        devices.save(TrustedDeviceEntity.builder()
                .id(UUID.randomUUID().toString())
                .accountId(accountId)
                .tokenHash(SessionService.hash(raw))
                .createdAt(now)
                .lastUsedAt(now)
                .expiresAt(now.plus(DEVICE_VALIDITY))
                .build());
        List<TrustedDeviceEntity> open = devices.findByAccountIdAndRevokedAtIsNullOrderByCreatedAtDesc(accountId);
        open.stream().skip(MAX_DEVICES_PER_ACCOUNT).forEach(old -> {
            old.setRevokedAt(now);
            devices.save(old);
        });
        return raw;
    }

    /** Ends every trusted device of the account except the one this browser carries (if any). */
    @Transactional
    public void forgetDevices(String accountId, String keepRawToken) {
        Instant now = Instant.now();
        if (keepRawToken == null || keepRawToken.isBlank()) {
            devices.revokeAll(accountId, now);
        } else {
            devices.revokeAllExcept(accountId, SessionService.hash(keepRawToken), now);
        }
    }

    public ResponseCookie deviceCookie(String raw) {
        return ResponseCookie.from(DEVICE_COOKIE, raw)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Strict")
                .path(SessionService.COOKIE_PATH)
                .maxAge(DEVICE_VALIDITY)
                .build();
    }

    private Optional<TrustedDeviceEntity> trustedDevice(Optional<String> accountId, Client client, Instant now) {
        String raw = client.deviceToken();
        if (accountId.isEmpty() || raw == null || raw.isBlank() || raw.length() > 128) {
            return Optional.empty();
        }
        return devices.findByTokenHash(SessionService.hash(raw))
                .filter(d -> d.getAccountId().equals(accountId.get()) && d.isValidAt(now));
    }

    // ---------------------------------------------------------------- housekeeping

    @Scheduled(fixedDelayString = "${sitesecurity.sign-in.cleanup-interval-ms:3600000}", initialDelay = 60_000)
    @Transactional
    public void cleanUp() {
        Instant now = Instant.now();
        failures.deleteOlderThan(now.minus(Duration.ofDays(1)));
        devices.deleteFinishedBefore(now.minus(Duration.ofDays(1)));
    }

    // ---------------------------------------------------------------- keys

    String emailKey(String email) {
        return mac("email:" + OperatorService.normalizeEmail(email == null ? "" : email));
    }

    String clientKey(Client client) {
        return mac("client:" + (client.address() == null ? "unknown" : client.address()));
    }

    private String mac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }
}

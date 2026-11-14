package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.springframework.stereotype.Component;

/**
 * Argon2id password hashing (RFC 9106) with the Bouncy Castle lightweight API.
 *
 * <p>Stored format (PHC string): {@code $argon2id$v=19$m=19456,t=2,p=1$<salt>$<hash>},
 * salt and hash in standard Base64 without padding. Parameters are read back
 * only from our own stored hashes, never from a request, and are bounded before
 * any work is done. At most four hashes run at the same time (each one needs
 * about 19 MiB of memory). A failed sign-in for an unknown account does the
 * same work as a real one ({@link #verifyAgainstDummy}).</p>
 */
@Component
public class PasswordHasher {

    static final int MEMORY_KIB = 19_456;
    static final int ITERATIONS = 2;
    static final int PARALLELISM = 1;
    static final int SALT_BYTES = 16;
    static final int HASH_BYTES = 32;
    static final int MAX_CONCURRENT_HASHES = 4;

    // Bounds for parameters read from a stored hash (they allow a later change of the defaults).
    static final int MIN_MEMORY_KIB = 8_192;
    static final int MAX_MEMORY_KIB = 65_536;
    static final int MAX_ITERATIONS = 10;
    static final int MAX_PARALLELISM = 4;
    static final int MIN_SALT_BYTES = 16;
    static final int MAX_SALT_BYTES = 64;
    static final int MIN_HASH_BYTES = 16;
    static final int MAX_HASH_BYTES = 64;
    /** Longest password accepted, in UTF-8 bytes (the policy allows 128 characters). */
    static final int MAX_PASSWORD_BYTES = 512;

    private static final Pattern PHC = Pattern.compile(
            "\\$argon2id\\$v=(\\d{1,3})\\$m=(\\d{1,6}),t=(\\d{1,2}),p=(\\d{1,2})\\$([A-Za-z0-9+/]{1,128})\\$([A-Za-z0-9+/]{1,128})");
    private static final Base64.Encoder B64 = Base64.getEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODER = Base64.getDecoder();

    private final SecureRandom random = new SecureRandom();
    private final Semaphore permits = new Semaphore(MAX_CONCURRENT_HASHES, true);
    private volatile String dummyHash;

    /** A new PHC string for this password, with the current parameters and a fresh salt. */
    public String hash(String password) {
        byte[] passwordBytes = passwordBytes(password);
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = withPermit(() -> argon2id(passwordBytes, salt, MEMORY_KIB, ITERATIONS, PARALLELISM, HASH_BYTES));
        return "$argon2id$v=19$m=" + MEMORY_KIB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
                + "$" + B64.encodeToString(salt) + "$" + B64.encodeToString(hash);
    }

    /**
     * True when the password matches the stored hash. A stored value that is not a
     * valid, in-bounds Argon2id PHC string never matches (and still costs a hash).
     */
    public boolean verify(String password, String storedHash) {
        Optional<Parsed> parsed = parse(storedHash);
        if (parsed.isEmpty() || password == null || utf8Length(password) > MAX_PASSWORD_BYTES) {
            verifyAgainstDummy(password);
            return false;
        }
        Parsed p = parsed.get();
        byte[] passwordBytes = passwordBytes(password);
        byte[] computed = withPermit(() -> argon2id(passwordBytes, p.salt(), p.memoryKib(), p.iterations(),
                p.parallelism(), p.hash().length));
        return MessageDigest.isEqual(computed, p.hash());
    }

    /** The same work as a real verification, for an account that does not exist or cannot sign in. */
    public void verifyAgainstDummy(String password) {
        String dummy = dummyHash;
        if (dummy == null) {
            dummy = hash("dummy-password-for-constant-work");
            dummyHash = dummy;
        }
        Parsed p = parse(dummy).orElseThrow();
        String safe = password == null || utf8Length(password) > MAX_PASSWORD_BYTES ? "" : password;
        byte[] passwordBytes = passwordBytes(safe);
        withPermit(() -> argon2id(passwordBytes, p.salt(), p.memoryKib(), p.iterations(), p.parallelism(), p.hash().length));
    }

    /** True when the stored hash uses other parameters than the current ones (rehash at next sign-in). */
    public boolean needsRehash(String storedHash) {
        return parse(storedHash)
                .map(p -> p.memoryKib() != MEMORY_KIB || p.iterations() != ITERATIONS
                        || p.parallelism() != PARALLELISM || p.salt().length != SALT_BYTES
                        || p.hash().length != HASH_BYTES)
                .orElse(true);
    }

    /** Strict parse: argon2id only, version 19 only, every parameter and length bounded. */
    static Optional<Parsed> parse(String phc) {
        if (phc == null || phc.length() > 255) {
            return Optional.empty();
        }
        Matcher m = PHC.matcher(phc);
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            int version = Integer.parseInt(m.group(1));
            int memory = Integer.parseInt(m.group(2));
            int iterations = Integer.parseInt(m.group(3));
            int parallelism = Integer.parseInt(m.group(4));
            byte[] salt = B64_DECODER.decode(m.group(5));
            byte[] hash = B64_DECODER.decode(m.group(6));
            if (version != 0x13
                    || memory < MIN_MEMORY_KIB || memory > MAX_MEMORY_KIB || memory < 8 * parallelism
                    || iterations < 1 || iterations > MAX_ITERATIONS
                    || parallelism < 1 || parallelism > MAX_PARALLELISM
                    || salt.length < MIN_SALT_BYTES || salt.length > MAX_SALT_BYTES
                    || hash.length < MIN_HASH_BYTES || hash.length > MAX_HASH_BYTES) {
                return Optional.empty();
            }
            return Optional.of(new Parsed(memory, iterations, parallelism, salt, hash));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Raw Argon2id (version 0x13). Package-private for the RFC 9106 test vector. */
    static byte[] argon2id(byte[] password, byte[] salt, int memoryKib, int iterations, int parallelism,
            int outputLength, byte[] secret, byte[] associatedData) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(memoryKib)
                .withIterations(iterations)
                .withParallelism(parallelism)
                .withSalt(salt);
        if (secret != null) {
            builder.withSecret(secret);
        }
        if (associatedData != null) {
            builder.withAdditional(associatedData);
        }
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());
        byte[] out = new byte[outputLength];
        generator.generateBytes(password, out, 0, out.length);
        return out;
    }

    private static byte[] argon2id(byte[] password, byte[] salt, int memoryKib, int iterations, int parallelism,
            int outputLength) {
        return argon2id(password, salt, memoryKib, iterations, parallelism, outputLength, null, null);
    }

    /** All four hashing slots stayed busy: the caller answers 503 instead of queueing. */
    public static class HashingBusyException extends RuntimeException {
        public HashingBusyException() {
            super("Too many sign-in attempts right now. Try again in a moment.");
        }
    }

    private byte[] withPermit(java.util.function.Supplier<byte[]> work) {
        try {
            // Fail fast: a flood of sign-ins must not pile up waiting threads (and their database connections).
            if (!permits.tryAcquire(2, TimeUnit.SECONDS)) {
                throw new HashingBusyException();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HashingBusyException();
        }
        try {
            return work.get();
        } finally {
            permits.release();
        }
    }

    /** Unicode NFC, so the same password typed on different systems gives the same bytes. */
    private static byte[] passwordBytes(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Missing password");
        }
        byte[] bytes = Normalizer.normalize(password, Normalizer.Form.NFC).getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_PASSWORD_BYTES) {
            throw new IllegalArgumentException("Password too long");
        }
        return bytes;
    }

    private static int utf8Length(String value) {
        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    record Parsed(int memoryKib, int iterations, int parallelism, byte[] salt, byte[] hash) {
    }

    int availablePermits() {
        return permits.availablePermits();
    }
}

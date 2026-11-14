package com.infineonbit.sustainablefarm.modules.sitesecurity.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    private static byte[] fill(int length, int value) {
        byte[] bytes = new byte[length];
        Arrays.fill(bytes, (byte) value);
        return bytes;
    }

    @Test
    void argon2id_shouldMatchTheRfc9106TestVector() {
        // RFC 9106, section 5.3: Argon2id, version 0x13, m=32 KiB, t=3, p=4, tag length 32.
        byte[] tag = PasswordHasher.argon2id(fill(32, 0x01), fill(16, 0x02), 32, 3, 4, 32,
                fill(8, 0x03), fill(12, 0x04));

        assertEquals("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659",
                HexFormat.of().formatHex(tag));
    }

    @Test
    void argon2id_shouldNotMatchTheVector_whenAnyInputChanges() {
        String expected = "0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659";
        byte[] otherSalt = PasswordHasher.argon2id(fill(32, 0x01), fill(16, 0x09), 32, 3, 4, 32, fill(8, 0x03), fill(12, 0x04));
        byte[] otherPasses = PasswordHasher.argon2id(fill(32, 0x01), fill(16, 0x02), 32, 2, 4, 32, fill(8, 0x03), fill(12, 0x04));

        assertNotEquals(expected, HexFormat.of().formatHex(otherSalt));
        assertNotEquals(expected, HexFormat.of().formatHex(otherPasses));
    }

    @Test
    void hash_shouldProduceAPhcStringWithTheApprovedParameters() {
        String phc = hasher.hash("correct horse battery staple");

        assertTrue(phc.startsWith("$argon2id$v=19$m=19456,t=2,p=1$"), phc);
        PasswordHasher.Parsed parsed = PasswordHasher.parse(phc).orElseThrow();
        assertEquals(16, parsed.salt().length);
        assertEquals(32, parsed.hash().length);
        assertFalse(phc.contains("correct horse"));
    }

    @Test
    void sameP4ssword_shouldGiveDifferentHashes() {
        assertNotEquals(hasher.hash("same password 123"), hasher.hash("same password 123"));
    }

    @Test
    void verify_shouldAcceptTheRightPassword_andRefuseOthers() {
        String phc = hasher.hash("correct horse battery staple");

        assertTrue(hasher.verify("correct horse battery staple", phc));
        assertFalse(hasher.verify("correct horse battery stapl", phc));
        assertFalse(hasher.verify("Correct horse battery staple", phc));
        assertFalse(hasher.verify("", phc));
        assertFalse(hasher.verify(null, phc));
    }

    @Test
    void verify_shouldTreatUnicodeCompositionAsTheSamePassword() {
        String composed = "café au lait du matin";
        String decomposed = "café au lait du matin";

        assertTrue(hasher.verify(decomposed, hasher.hash(composed)));
    }

    @Test
    void verify_shouldRefuseTamperedOrOutOfBoundsHashes() {
        String phc = hasher.hash("correct horse battery staple");
        String salt = phc.split("\\$")[4];
        String hash = phc.split("\\$")[5];
        List<String> invalid = List.of(
                phc.replace("$argon2id$", "$argon2i$"),                       // other algorithm
                phc.replace("$argon2id$", "$argon2d$"),
                phc.replace("v=19", "v=16"),                                  // other version
                phc.replace("m=19456", "m=1048576"),                          // memory far too high (DoS)
                phc.replace("m=19456", "m=65537"),
                phc.replace("m=19456", "m=8191"),                             // too little memory
                phc.replace("t=2", "t=11"),                                   // too many iterations
                phc.replace("t=2", "t=0"),
                phc.replace("p=1", "p=5"),                                    // too much parallelism
                phc.replace("p=1", "p=0"),
                "$argon2id$v=19$m=19456,t=2,p=1$" + salt.substring(0, 10) + "$" + hash,   // salt too short
                "$argon2id$v=19$m=19456,t=2,p=1$" + salt + "$" + hash.substring(0, 12),   // output too short
                "$argon2id$v=19$m=19456,t=2,p=1$" + salt + "$" + hash + hash + hash,      // output too long
                phc + "$extra",
                "$argon2id$v=19$m=19456,t=2,p=1$" + salt + "$not*base64",
                "plain-text-password",
                "",
                "$pbkdf2-sha256$i=600000$abc$def");
        for (String candidate : invalid) {
            assertFalse(hasher.verify("correct horse battery staple", candidate), candidate);
            assertTrue(PasswordHasher.parse(candidate).isEmpty(), candidate);
        }
    }

    @Test
    void needsRehash_shouldFlagOtherParameters() {
        String current = hasher.hash("correct horse battery staple");
        String older = current.replace("m=19456,t=2,p=1", "m=8192,t=3,p=1");

        assertFalse(hasher.needsRehash(current));
        assertTrue(hasher.needsRehash(older));
        assertTrue(hasher.needsRehash("garbage"));
    }

    @Test
    void verifyAgainstDummy_shouldNotFail_forAnyInput() {
        hasher.verifyAgainstDummy("anything at all");
        hasher.verifyAgainstDummy(null);
        hasher.verifyAgainstDummy("x".repeat(10_000));
    }

    @Test
    void hash_shouldRefuseAnOversizedPassword() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("x".repeat(PasswordHasher.MAX_PASSWORD_BYTES + 1)));
        assertFalse(hasher.verify("x".repeat(PasswordHasher.MAX_PASSWORD_BYTES + 1), hasher.hash("correct horse battery staple")));
    }

    @Test
    void atMostFourHashes_shouldRunAtTheSameTime() throws Exception {
        AtomicInteger lowestFreePermits = new AtomicInteger(PasswordHasher.MAX_CONCURRENT_HASHES);
        ExecutorService pool = Executors.newFixedThreadPool(16);
        try {
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                results.add(pool.submit(() -> {
                    String phc = hasher.hash("correct horse battery staple");
                    lowestFreePermits.accumulateAndGet(hasher.availablePermits(), Math::min);
                    return phc;
                }));
            }
            for (Future<String> result : results) {
                assertTrue(hasher.verify("correct horse battery staple", result.get()));
            }
        } finally {
            pool.shutdown();
        }
        assertTrue(lowestFreePermits.get() >= 0);
        assertEquals(PasswordHasher.MAX_CONCURRENT_HASHES, hasher.availablePermits());
    }
}

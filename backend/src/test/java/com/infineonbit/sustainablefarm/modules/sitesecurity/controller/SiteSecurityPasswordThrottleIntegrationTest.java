package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestAsync;
import com.infineonbit.sustainablefarm.modules.sitesecurity.TestMail;
import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.EmailDeliveryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.FailedSignInRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SignInFailureRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityAuditEventRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecuritySettingRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.TrustedDeviceRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SignInThrottle;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions.signedIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password sign-in limits: guessing is held back per client, per email and per
 * device, before any hashing, and no stranger can keep the owner out.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-throttle;DB_CLOSE_DELAY=-1",
        "app.mail.enabled=true",
        "SITESECURITY_OTP_SECRET=otp-secret-for-throttle-tests-0123456789abcdef",
        "sitesecurity.mail.retry-initial-delay-ms=3600000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityPasswordThrottleIntegrationTest {

    private static final String BASE = "/api/v1/sitesecurity";
    private static final String ADMIN = "throttle.admin@example.test";
    private static final String OWNER = "owner@example.test";
    private static final String PASSWORD = "correct horse battery staple";
    private static final String WRONG = "not the right passphrase";
    private static final String RESET_SUBJECT = "Réinitialisation de votre mot de passe Farm Security";

    @Autowired private MockMvc mvc;
    @Autowired private OperatorRepository accounts;
    @Autowired private OperatorSessionRepository sessions;
    @Autowired private AccountTokenRepository tokens;
    @Autowired private EmailDeliveryRepository deliveries;
    @Autowired private FailedSignInRepository failedCodes;
    @Autowired private SignInFailureRepository signInFailures;
    @Autowired private TrustedDeviceRepository devices;
    @Autowired private LoginCodeRepository loginCodes;
    @Autowired private SiteSecurityAuditEventRepository auditEvents;
    @Autowired private SiteSecuritySettingRepository settings;

    @Autowired @Qualifier(SiteSecurityExecutors.PUBLIC_REQUESTS) private ThreadPoolTaskExecutor publicRequests;
    @Autowired @Qualifier(SiteSecurityExecutors.ACCOUNT_EMAILS) private ThreadPoolTaskExecutor accountEmails;

    @MockitoBean
    private JavaMailSender mail;

    private String adminSession;
    private String ownerDevice;

    @BeforeEach
    void resetData() throws Exception {
        TestAsync.drain(publicRequests, accountEmails);
        TestMail.prepare(mail);
        settings.deleteAll();
        deliveries.deleteAll();
        tokens.deleteAll();
        failedCodes.deleteAll();
        signInFailures.deleteAll();
        devices.deleteAll();
        loginCodes.deleteAll();
        auditEvents.deleteAll();
        sessions.deleteAll();
        accounts.deleteAll();
        adminSession = TestSessions.open(accounts, sessions, ADMIN, SiteSecurityRole.ADMIN);
        mvc.perform(post(BASE + "/users").with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Owen Owner\",\"email\":\"" + OWNER + "\",\"emailConfirmation\":\"" + OWNER
                                + "\",\"role\":\"user\",\"category\":\"worker\",\"permissions\":[\"view_zones\"],\"zoneIds\":[\"open\"]}"))
                .andExpect(status().isCreated());
        String link = TestMail.token(TestMail.await(mail, OWNER, 1));
        MvcResult activated = mvc.perform(json(post(BASE + "/auth/activate"), "{\"token\":\"" + link + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk()).andReturn();
        ownerDevice = cookie(activated, SignInThrottle.DEVICE_COOKIE);
        assertNotNull(ownerDevice, "activation trusts the browser");
    }

    // ------------------------------------------------------------------ helpers

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** A sign-in through nginx (peer in the Compose network, client address in X-Real-IP). */
    private ResultActions login(String email, String password, String ip, String device) throws Exception {
        MockHttpServletRequestBuilder request = json(post(BASE + "/auth/login"),
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .with(r -> {
                    r.setRemoteAddr("172.18.0.5");
                    return r;
                })
                .header("X-Real-IP", ip);
        if (device != null) {
            request.cookie(new Cookie(SignInThrottle.DEVICE_COOKIE, device));
        }
        return mvc.perform(request);
    }

    private static String cookie(MvcResult result, String name) {
        List<String> headers = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        return headers.stream().filter(h -> h.startsWith(name + "="))
                .map(h -> h.substring(name.length() + 1, h.indexOf(';'))).findFirst().orElse(null);
    }

    /** One failure from each of {@code count} different addresses: a distributed attack on the email. */
    private void distributedFailures(String email, int count) throws Exception {
        for (int i = 0; i < count; i++) {
            login(email, WRONG + i, "203.0.113." + (i + 1), null).andExpect(status().isUnauthorized());
        }
    }

    private MimeMessage awaitReset(int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            List<MimeMessage> resets = TestMail.to(mail, OWNER).stream()
                    .filter(m -> RESET_SUBJECT.equals(TestMail.subject(m))).toList();
            if (resets.size() >= count) {
                return resets.get(count - 1);
            }
            Thread.sleep(25);
        }
        throw new AssertionError("No reset email #" + count);
    }

    // ------------------------------------------------------------------ tests

    @Test
    void repeatedFailures_fromOneAddress_shouldOnlyDelayThatAddress() throws Exception {
        for (int i = 0; i < 3; i++) {
            login(OWNER, WRONG, "198.51.100.1", null).andExpect(status().isUnauthorized());
        }
        login(OWNER, PASSWORD, "198.51.100.1", null).andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "30"));
        long recorded = signInFailures.count();
        login(OWNER, WRONG, "198.51.100.1", null).andExpect(status().isTooManyRequests());
        assertEquals(recorded, signInFailures.count(), "a refused attempt is not even tried (no hashing, no row)");

        // Once the delay has passed (rows aged), one more try is allowed, then the next delay is longer.
        signInFailures.findAll().forEach(f -> agedCopy(f, 31));
        login(OWNER, WRONG, "198.51.100.1", null).andExpect(status().isUnauthorized());
        login(OWNER, PASSWORD, "198.51.100.1", null).andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "60"));

        // The owner, from anywhere else, is not affected.
        login(OWNER, PASSWORD, "192.0.2.50", null).andExpect(status().isOk());
    }

    @Test
    void distributedAttack_shouldHoldBackNewBrowsers_butNeverTheOwnersBrowser() throws Exception {
        distributedFailures(OWNER, 20);
        String accountId = accounts.findByEmail(OWNER).orElseThrow().getId();
        assertTrue(auditEvents.findByTargetIdOrderByOccurredAtAsc(accountId).stream()
                .anyMatch(e -> e.getAction() == SiteSecurityAuditAction.ACCOUNT_SIGN_IN_BLOCKED));

        // Even the right password from a new browser waits: guessing is over for strangers.
        login(OWNER, PASSWORD, "203.0.113.200", null).andExpect(status().isTooManyRequests());
        // The owner's browser keeps signing in, from any address.
        login(OWNER, PASSWORD, "203.0.113.201", ownerDevice).andExpect(status().isOk());
        login(OWNER, PASSWORD, "192.0.2.9", ownerDevice).andExpect(status().isOk());
    }

    @Test
    void stolenDeviceCookie_shouldGiveOnlyFiveGuesses() throws Exception {
        distributedFailures(OWNER, 20);
        for (int i = 0; i < 5; i++) {
            login(OWNER, WRONG + "x" + i, "198.51.100." + (100 + i), ownerDevice).andExpect(status().isUnauthorized());
        }
        login(OWNER, PASSWORD, "198.51.100.120", ownerDevice).andExpect(status().isTooManyRequests());
    }

    @Test
    void deviceCookie_ofAnotherAccount_orForged_shouldGiveNoExemption() throws Exception {
        TestSessions.open(accounts, sessions, "other@example.test", SiteSecurityRole.READ);
        distributedFailures(OWNER, 20);
        login(OWNER, PASSWORD, "198.51.100.30", "forged-device-value").andExpect(status().isTooManyRequests());
        distributedFailures("other@example.test", 20);
        login("other@example.test", PASSWORD, "198.51.100.31", ownerDevice).andExpect(status().isTooManyRequests());
    }

    @Test
    void passwordReset_shouldStayAvailable_liftTheLimits_andTrustTheBrowser() throws Exception {
        distributedFailures(OWNER, 20);
        login(OWNER, PASSWORD, "198.51.100.40", null).andExpect(status().isTooManyRequests());

        // The owner asks for a reset; later a stranger asks again: the owner's link stays valid.
        mvc.perform(json(post(BASE + "/auth/forgot-password"), "{\"email\":\"" + OWNER + "\"}")).andExpect(status().isAccepted());
        String ownerLink = TestMail.token(awaitReset(1));
        tokens.findAll().stream().filter(t -> "PASSWORD_RESET".equals(t.getPurpose()))
                .forEach(t -> tokens.save(aged(t, 6 * 60)));
        mvc.perform(json(post(BASE + "/auth/forgot-password"), "{\"email\":\"" + OWNER + "\"}")).andExpect(status().isAccepted());
        awaitReset(2);
        assertEquals(2, tokens.findAll().stream().filter(t -> "PASSWORD_RESET".equals(t.getPurpose()) && t.getRevokedAt() == null).count());

        MvcResult reset = mvc.perform(json(post(BASE + "/auth/reset-password"),
                "{\"token\":\"" + ownerLink + "\",\"password\":\"" + PASSWORD + " again\"}"))
                .andExpect(status().isNoContent()).andReturn();
        String newDevice = cookie(reset, SignInThrottle.DEVICE_COOKIE);
        assertNotNull(newDevice, "the reset trusts the browser it was done in");

        login(OWNER, PASSWORD + " again", "198.51.100.41", newDevice).andExpect(status().isOk());
        // Every limit on the email is lifted, and the old device ended with the reset.
        login(OWNER, PASSWORD + " again", "198.51.100.42", null).andExpect(status().isOk());
        assertFalse(devices.findByTokenHash(SessionService.hash(ownerDevice)).orElseThrow().isValidAt(Instant.now()));
    }

    @Test
    void spraying_fromOneAddress_shouldStopThatAddress_only() throws Exception {
        for (int i = 0; i < 30; i++) {
            login("nobody" + i + "@example.test", WRONG, "198.51.100.60", null).andExpect(status().isUnauthorized());
        }
        login("nobody-else@example.test", WRONG, "198.51.100.60", null).andExpect(status().isTooManyRequests());
        login(OWNER, PASSWORD, "198.51.100.60", null).andExpect(status().isTooManyRequests());
        login(OWNER, PASSWORD, "198.51.100.60", ownerDevice).andExpect(status().isOk());
        login(OWNER, PASSWORD, "198.51.100.61", null).andExpect(status().isOk());
    }

    @Test
    void unknownEmail_shouldBeLimitedExactlyLikeARealOne() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("ghost@example.test", WRONG, "198.51.100.70", null).andExpect(status().isUnauthorized());
            login(OWNER, WRONG, "198.51.100.71", null).andExpect(status().isUnauthorized());
        }
        String ghost = login("ghost@example.test", WRONG, "198.51.100.70", null).andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getContentAsString();
        String real = login(OWNER, WRONG, "198.51.100.71", null).andExpect(status().isTooManyRequests())
                .andReturn().getResponse().getContentAsString();
        assertEquals(ghost, real);
        assertEquals("Too many sign-in attempts. Wait a few minutes, use a browser you signed in with before, or reset your password.",
                JsonPath.read(real, "$.message"));
    }

    @Test
    void forwardedAddress_fromAPublicPeer_shouldBeIgnored() throws Exception {
        for (int i = 0; i < 4; i++) {
            int n = i;
            mvc.perform(json(post(BASE + "/auth/login"), "{\"email\":\"" + OWNER + "\",\"password\":\"" + WRONG + "\"}")
                    .with(r -> {
                        r.setRemoteAddr("203.0.113.99");
                        return r;
                    })
                    .header("X-Real-IP", "10.0.0." + n))
                    .andExpect(n < 3 ? status().isUnauthorized() : status().isTooManyRequests());
        }
    }

    // ------------------------------------------------------------------ row ageing (time travel)

    private void agedCopy(com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SignInFailureEntity f, int seconds) {
        signInFailures.save(com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SignInFailureEntity.builder()
                .id(f.getId()).emailKey(f.getEmailKey()).clientKey(f.getClientKey()).deviceId(f.getDeviceId())
                .occurredAt(f.getOccurredAt().minusSeconds(seconds)).build());
    }

    private static AccountTokenEntity aged(AccountTokenEntity t, int seconds) {
        return AccountTokenEntity.builder().id(t.getId()).accountId(t.getAccountId()).purpose(t.getPurpose())
                .tokenHash(t.getTokenHash()).createdAt(t.getCreatedAt().minusSeconds(seconds)).expiresAt(t.getExpiresAt())
                .usedAt(t.getUsedAt()).revokedAt(t.getRevokedAt()).confirmedAt(t.getConfirmedAt()).createdBy(t.getCreatedBy())
                .build();
    }
}

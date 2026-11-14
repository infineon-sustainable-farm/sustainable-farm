package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestMail;
import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountTokenEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditEventEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.AccountTokenRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.EmailDeliveryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.FailedSignInRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityAuditEventRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecuritySettingRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.AccountMailSender;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions.signedIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The whole account journey: invitation, activation, password sign-in and recovery, tracking, migration rules. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-accounts;DB_CLOSE_DELAY=-1",
        "app.mail.enabled=true",
        "SITESECURITY_OTP_SECRET=otp-secret-for-account-tests-0123456789abcdef",
        "sitesecurity.mail.retry-initial-delay-ms=3600000"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SiteSecurityAccountJourneyIntegrationTest {

    private static final String BASE = "/api/v1/sitesecurity";
    private static final String ADMIN = "first.admin@example.test";
    private static final String USER = "ada.worker@example.test";
    private static final String PASSWORD = "correct horse battery staple";
    private static final String OTHER_PASSWORD = "a different long passphrase";
    private static final String GENERIC_LOGIN_ERROR = "Email or password is incorrect.";
    private static final String GENERIC_LINK_ERROR = "This link is invalid or has expired.";

    @Autowired private MockMvc mvc;
    @Autowired private OperatorRepository accounts;
    @Autowired private OperatorSessionRepository sessions;
    @Autowired private AccountTokenRepository tokens;
    @Autowired private EmailDeliveryRepository deliveries;
    @Autowired private FailedSignInRepository failures;
    @Autowired private LoginCodeRepository loginCodes;
    @Autowired private SiteSecurityAuditEventRepository auditEvents;
    @Autowired private SiteSecuritySettingRepository settings;

    @MockitoBean
    private JavaMailSender mail;

    private String adminSession;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier(com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors.PUBLIC_REQUESTS)
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor publicRequests;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier(com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors.ACCOUNT_EMAILS)
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor accountEmails;

    @Autowired
    private com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SignInFailureRepository signInFailures;

    @Autowired
    private com.infineonbit.sustainablefarm.modules.sitesecurity.repository.TrustedDeviceRepository trustedDevices;

    @BeforeEach
    void resetData() {
        com.infineonbit.sustainablefarm.modules.sitesecurity.TestAsync.drain(publicRequests, accountEmails);
        TestMail.prepare(mail);
        settings.deleteAll();
        deliveries.deleteAll();
        tokens.deleteAll();
        failures.deleteAll();
        signInFailures.deleteAll();
        trustedDevices.deleteAll();
        loginCodes.deleteAll();
        auditEvents.deleteAll();
        sessions.deleteAll();
        accounts.deleteAll();
        adminSession = TestSessions.open(accounts, sessions, ADMIN, SiteSecurityRole.ADMIN);
    }

    // ------------------------------------------------------------------ helpers

    private ResultActions postJson(String path, String json, String session) throws Exception {
        var request = post(BASE + path).header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content(json);
        if (session != null) {
            request.cookie(new Cookie(SessionService.COOKIE_NAME, session));
        }
        return mvc.perform(request);
    }

    private String invite(String email, String role, String permissions, String zones) throws Exception {
        String body = mvc.perform(post(BASE + "/users").with(signedIn(adminSession))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ada Worker\",\"email\":\"" + email + "\",\"emailConfirmation\":\"" + email
                                + "\",\"role\":\"" + role + "\",\"category\":\"worker\",\"permissions\":" + permissions
                                + ",\"zoneIds\":" + zones + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String inviteUser() throws Exception {
        return invite(USER, "user", "[\"view_zones\"]", "[\"open\"]");
    }

    private String linkTo(String email, int count) {
        return TestMail.token(TestMail.await(mail, email, count));
    }

    private static String cookieOf(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(header, "no session cookie");
        return header.substring(header.indexOf('=') + 1, header.indexOf(';'));
    }

    private String activate(String token, String password) throws Exception {
        MvcResult result = postJson("/auth/activate", "{\"token\":\"" + token + "\",\"password\":\"" + password + "\"}", null)
                .andExpect(status().isOk()).andReturn();
        return cookieOf(result);
    }

    private ResultActions login(String email, String password, String session) throws Exception {
        return postJson("/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}", session);
    }

    private String loginOk(String email, String password) throws Exception {
        return cookieOf(login(email, password, null).andExpect(status().isOk()).andReturn());
    }

    private String statusOf(String id) throws Exception {
        String body = mvc.perform(get(BASE + "/users").with(signedIn(adminSession))).andReturn().getResponse().getContentAsString();
        List<String> statuses = JsonPath.read(body, "$[?(@.id == '" + id + "')].status");
        return statuses.get(0);
    }

    private ResultActions session(String cookie) throws Exception {
        return mvc.perform(get(BASE + "/session").cookie(new Cookie(SessionService.COOKIE_NAME, cookie)));
    }

    private List<SiteSecurityAuditAction> actionsFor(String id) {
        return auditEvents.findByTargetIdOrderByOccurredAtAsc(id).stream().map(SiteSecurityAuditEventEntity::getAction).toList();
    }

    // ------------------------------------------------------------------ invitation and activation

    @Test
    void invitedAccount_shouldBePendingWithoutAccess_andItsEmailSubmittedNotDelivered() throws Exception {
        String id = inviteUser();
        MimeMessage invitation = TestMail.await(mail, USER, 1);

        assertEquals("Invitation à Farm Security", TestMail.subject(invitation));
        assertTrue(TestMail.html(invitation).contains("/sitesecurity/activate#token="));
        assertEquals(1, invitation.getHeader(AccountMailSender.DELIVERY_HEADER).length);
        assertEquals("pending", statusOf(id));
        OperatorEntity account = accounts.findById(id).orElseThrow();
        assertFalse(account.isActive());
        assertFalse(account.hasPassword());
        assertTrue(account.getZoneIds().contains("open"));

        // Submission and delivery are tracked apart: accepted by the provider is not delivered.
        long deadline = System.currentTimeMillis() + 5000;
        String submission = "";
        while (System.currentTimeMillis() < deadline && !submission.equals("submitted")) {
            String body = mvc.perform(get(BASE + "/users").with(signedIn(adminSession))).andReturn().getResponse().getContentAsString();
            submission = ((List<String>) JsonPath.read(body, "$[?(@.id == '" + id + "')].emailDelivery.submission")).get(0);
            Thread.sleep(25);
        }
        assertEquals("submitted", submission);
        mvc.perform(get(BASE + "/users").with(signedIn(adminSession)))
                .andExpect(jsonPath("$[?(@.id == '" + id + "')].emailDelivery.delivery").value("unknown"));

        // No access before activation, whatever the password.
        login(USER, PASSWORD, null).andExpect(status().isUnauthorized());
    }

    @Test
    void inspect_shouldShowWhoTheLinkIsFor_andConfirmOnlyMarksActivationStarted() throws Exception {
        String id = inviteUser();
        String token = linkTo(USER, 1);

        postJson("/auth/tokens/inspect", "{\"token\":\"" + token + "\"}", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purpose").value("invitation"))
                .andExpect(jsonPath("$.fullName").value("Ada"))
                .andExpect(jsonPath("$.maskedEmail").value("a***@e***.test"));
        assertEquals("pending", statusOf(id));

        postJson("/auth/invitations/confirm", "{\"token\":\"" + token + "\"}", null).andExpect(status().isNoContent());
        postJson("/auth/invitations/confirm", "{\"token\":\"" + token + "\"}", null).andExpect(status().isNoContent());

        assertEquals("activation_started", statusOf(id));
        assertFalse(accounts.findById(id).orElseThrow().isActive());
        login(USER, PASSWORD, null).andExpect(status().isUnauthorized());
        assertEquals(1, actionsFor(id).stream().filter(a -> a == SiteSecurityAuditAction.INVITATION_CONFIRMED).count());
    }

    @Test
    void activation_shouldAcceptTheInvitation_signIn_andBeRecorded() throws Exception {
        String id = inviteUser();
        String token = linkTo(USER, 1);

        String cookie = activate(token, PASSWORD);

        session(cookie).andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.passwordSet").value(true))
                .andExpect(jsonPath("$.permissions[0]").value("view_zones"));
        assertEquals("active", statusOf(id));
        OperatorEntity account = accounts.findById(id).orElseThrow();
        assertNotNull(account.getActivatedAt());
        assertNotNull(account.getFirstLoginAt());
        assertTrue(account.getPasswordHash().startsWith("$argon2id$v=19$m=19456,t=2,p=1$"));
        assertTrue(actionsFor(id).containsAll(List.of(SiteSecurityAuditAction.ACCOUNT_INVITED,
                SiteSecurityAuditAction.ACCOUNT_ACTIVATED, SiteSecurityAuditAction.ACCOUNT_FIRST_SIGN_IN)));
        // The other admins are told that the account was activated.
        assertEquals("Un compte Farm Security a été activé", TestMail.subject(TestMail.await(mail, ADMIN, 1)));

        // Afterwards: normal sign-in with email and password.
        session(loginOk(USER, PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void links_shouldWorkOnce() throws Exception {
        inviteUser();
        String token = linkTo(USER, 1);
        activate(token, PASSWORD);

        postJson("/auth/activate", "{\"token\":\"" + token + "\",\"password\":\"" + OTHER_PASSWORD + "\"}", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(GENERIC_LINK_ERROR));
        postJson("/auth/tokens/inspect", "{\"token\":\"" + token + "\"}", null).andExpect(status().isBadRequest());
        login(USER, OTHER_PASSWORD, null).andExpect(status().isUnauthorized());
    }

    @Test
    void resend_andCopiedLink_shouldEachReplaceThePreviousLink() throws Exception {
        String id = inviteUser();
        String first = linkTo(USER, 1);

        mvc.perform(post(BASE + "/users/" + id + "/invitation").with(signedIn(adminSession))).andExpect(status().isOk());
        String second = linkTo(USER, 2);
        postJson("/auth/tokens/inspect", "{\"token\":\"" + first + "\"}", null).andExpect(status().isBadRequest());

        String copied = mvc.perform(post(BASE + "/users/" + id + "/invitation/link").with(signedIn(adminSession)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String link = JsonPath.read(copied, "$.link");
        String third = link.substring(link.indexOf("#token=") + 7);
        postJson("/auth/tokens/inspect", "{\"token\":\"" + second + "\"}", null).andExpect(status().isBadRequest());
        assertEquals(2, TestMail.to(mail, USER).size(), "the copied link is not emailed");

        activate(third, PASSWORD);
        assertTrue(actionsFor(id).containsAll(List.of(SiteSecurityAuditAction.INVITATION_RESENT,
                SiteSecurityAuditAction.INVITATION_LINK_COPIED)));
    }

    @Test
    void revokedInvitation_shouldBeShownRevoked_andItsLinkDead() throws Exception {
        String id = inviteUser();
        String token = linkTo(USER, 1);

        mvc.perform(post(BASE + "/users/" + id + "/invitation/revoke").with(signedIn(adminSession)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("revoked"));

        postJson("/auth/activate", "{\"token\":\"" + token + "\",\"password\":\"" + PASSWORD + "\"}", null)
                .andExpect(status().isBadRequest());
        login(USER, PASSWORD, null).andExpect(status().isUnauthorized());
    }

    @Test
    void expiredInvitation_shouldBeShownExpired_andItsLinkDead() throws Exception {
        String id = inviteUser();
        String token = linkTo(USER, 1);
        for (AccountTokenEntity t : tokens.findAll()) {
            t.setExpiresAt(Instant.now().minusSeconds(1));
            tokens.save(t);
        }

        assertEquals("expired", statusOf(id));
        postJson("/auth/tokens/inspect", "{\"token\":\"" + token + "\"}", null).andExpect(status().isBadRequest());
        mvc.perform(get(BASE + "/users/summary").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.expired").value(1));
    }

    @Test
    void activation_shouldRefuseAWeakPassword_andKeepTheLinkUsable() throws Exception {
        inviteUser();
        String token = linkTo(USER, 1);

        postJson("/auth/activate", "{\"token\":\"" + token + "\",\"password\":\"short\"}", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
        postJson("/auth/activate", "{\"token\":\"" + token + "\",\"password\":\"" + USER + "\"}", null)
                .andExpect(status().isBadRequest());

        activate(token, PASSWORD);
    }

    // ------------------------------------------------------------------ password sign-in

    @Test
    void login_shouldGiveOneAnswer_forEveryFailure() throws Exception {
        String pendingId = invite("pending.person@example.test", "user", "[]", "[]");
        inviteUser();
        activate(linkTo(USER, 1), PASSWORD);
        String disabledId = invite("disabled.person@example.test", "user", "[]", "[]");
        activate(linkTo("disabled.person@example.test", 1), PASSWORD);
        mvc.perform(post(BASE + "/users/" + disabledId + "/disable").with(signedIn(adminSession))).andExpect(status().isOk());

        for (String[] attempt : List.of(new String[] {"nobody@example.test", PASSWORD}, new String[] {USER, OTHER_PASSWORD},
                new String[] {"pending.person@example.test", PASSWORD}, new String[] {"disabled.person@example.test", PASSWORD},
                new String[] {ADMIN, PASSWORD})) {
            login(attempt[0], attempt[1], null).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value(GENERIC_LOGIN_ERROR));
        }
        assertEquals("pending", statusOf(pendingId));
    }

    @Test
    void login_shouldCloseTheSessionTheBrowserAlreadyCarries() throws Exception {
        inviteUser();
        String first = activate(linkTo(USER, 1), PASSWORD);

        String second = cookieOf(login(USER, PASSWORD, first).andExpect(status().isOk()).andReturn());

        session(first).andExpect(status().isUnauthorized());
        session(second).andExpect(status().isOk());
    }

    @Test
    void wrongPasswords_shouldSlowThisClientDown_withoutLockingTheAccount() throws Exception {
        inviteUser();
        activate(linkTo(USER, 1), PASSWORD);

        // 3 free failures, then this client waits (429 + Retry-After) before trying again.
        for (int i = 0; i < 3; i++) {
            login(USER, OTHER_PASSWORD, null).andExpect(status().isUnauthorized());
        }
        login(USER, PASSWORD, null).andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string(HttpHeaders.RETRY_AFTER, "30"));
        // No account lock: the right password from another address signs in at once.
        mvc.perform(post(BASE + "/auth/login").header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                        .header("X-Real-IP", "198.51.100.7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + USER + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    private MimeMessage awaitSubject(String to, String subject) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline) {
            for (MimeMessage message : TestMail.to(mail, to)) {
                if (subject.equals(TestMail.subject(message))) {
                    return message;
                }
            }
            Thread.sleep(25);
        }
        throw new AssertionError("No '" + subject + "' email to " + to);
    }

    // ------------------------------------------------------------------ recovery and change

    @Test
    void forgotPassword_shouldAnswerTheSame_andOnlyEmailActiveAccounts() throws Exception {
        invite("pending.person@example.test", "user", "[]", "[]");
        inviteUser();
        activate(linkTo(USER, 1), PASSWORD);
        TestMail.clear(mail);

        String unknown = postJson("/auth/forgot-password", "{\"email\":\"nobody@example.test\"}", null)
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String pending = postJson("/auth/forgot-password", "{\"email\":\"pending.person@example.test\"}", null)
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String active = postJson("/auth/forgot-password", "{\"email\":\"" + USER + "\"}", null)
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertEquals(unknown, pending);
        assertEquals(unknown, active);
        awaitSubject(USER, "Réinitialisation de votre mot de passe Farm Security");
        Thread.sleep(300);
        assertTrue(TestMail.to(mail, "nobody@example.test").isEmpty());
        assertTrue(TestMail.to(mail, "pending.person@example.test").isEmpty());
    }

    @Test
    void reset_shouldEndEverySession_beSingleUse_andNotify() throws Exception {
        inviteUser();
        String sessionCookie = activate(linkTo(USER, 1), PASSWORD);
        postJson("/auth/forgot-password", "{\"email\":\"" + USER + "\"}", null).andExpect(status().isAccepted());
        String token = TestMail.token(awaitSubject(USER, "Réinitialisation de votre mot de passe Farm Security"));

        postJson("/auth/reset-password", "{\"token\":\"" + token + "\",\"password\":\"" + OTHER_PASSWORD + "\"}", null)
                .andExpect(status().isNoContent());

        session(sessionCookie).andExpect(status().isUnauthorized());
        login(USER, PASSWORD, null).andExpect(status().isUnauthorized());
        loginOk(USER, OTHER_PASSWORD);
        postJson("/auth/reset-password", "{\"token\":\"" + token + "\",\"password\":\"yet another long phrase\"}", null)
                .andExpect(status().isBadRequest());
        awaitSubject(USER, "Votre mot de passe Farm Security a été modifié");
    }

    @Test
    void changePassword_shouldNeedTheCurrentOne_andEndTheOtherSessions() throws Exception {
        inviteUser();
        String current = activate(linkTo(USER, 1), PASSWORD);
        String other = loginOk(USER, PASSWORD);

        postJson("/account/password", "{\"currentPassword\":\"wrong password here\",\"newPassword\":\"" + OTHER_PASSWORD + "\"}", current)
                .andExpect(status().isBadRequest());
        postJson("/account/password", "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + OTHER_PASSWORD + "\"}", current)
                .andExpect(status().isNoContent());

        session(current).andExpect(status().isOk());
        session(other).andExpect(status().isUnauthorized());
        loginOk(USER, OTHER_PASSWORD);
    }

    // ------------------------------------------------------------------ migration stages

    @Test
    void legacyAccount_shouldKeepEmailCodes_untilItSetsAPassword_thenPasswordOnly() throws Exception {
        String legacyEmail = "legacy.admin@example.test";
        TestSessions.open(accounts, sessions, legacyEmail, SiteSecurityRole.ADMIN);

        postJson("/auth/request-code", "{\"email\":\"" + legacyEmail + "\"}", null).andExpect(status().isAccepted());
        String code = TestMail.code(TestMail.await(mail, legacyEmail, 1));
        String codeSession = cookieOf(postJson("/auth/verify", "{\"email\":\"" + legacyEmail + "\",\"code\":\"" + code + "\"}", null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.passwordSet").value(false)).andReturn());

        // Stage 1: set a password in-session, with a fresh code.
        loginCodes.findAll().forEach(c -> {
            c.setCreatedAt(Instant.now().minusSeconds(120));
            loginCodes.save(c);
        });
        postJson("/auth/request-code", "{\"email\":\"" + legacyEmail + "\"}", null).andExpect(status().isAccepted());
        String fresh = TestMail.code(TestMail.await(mail, legacyEmail, 2));
        String wrong = fresh.equals("000000") ? "000001" : "000000";
        postJson("/account/password/setup", "{\"code\":\"" + wrong + "\",\"password\":\"" + PASSWORD + "\"}", codeSession)
                .andExpect(status().isBadRequest());
        postJson("/account/password/setup", "{\"code\":\"" + fresh + "\",\"password\":\"" + PASSWORD + "\"}", codeSession)
                .andExpect(status().isNoContent());

        // The code session stays; the account now signs in with its password only.
        session(codeSession).andExpect(status().isOk()).andExpect(jsonPath("$.passwordSet").value(true));
        loginCodes.findAll().forEach(c -> {
            c.setCreatedAt(Instant.now().minusSeconds(120));
            loginCodes.save(c);
        });
        TestMail.clear(mail);
        postJson("/auth/request-code", "{\"email\":\"" + legacyEmail + "\"}", null).andExpect(status().isAccepted());
        Thread.sleep(400);
        assertTrue(TestMail.to(mail, legacyEmail).isEmpty(), "no code for an account with a password");
        loginOk(legacyEmail, PASSWORD);
    }

    @Test
    void setup_shouldCountAWrongCode_againstTheCodesFiveAttempts() throws Exception {
        String legacyEmail = "legacy.reader@example.test";
        String session = TestSessions.open(accounts, sessions, legacyEmail, SiteSecurityRole.READ);
        postJson("/auth/request-code", "{\"email\":\"" + legacyEmail + "\"}", null).andExpect(status().isAccepted());
        String code = TestMail.code(TestMail.await(mail, legacyEmail, 1));
        String wrong = code.equals("000000") ? "000001" : "000000";

        postJson("/account/password/setup", "{\"code\":\"" + wrong + "\",\"password\":\"" + PASSWORD + "\"}", session)
                .andExpect(status().isBadRequest());

        assertEquals(1, loginCodes.findAll().get(0).getAttempts());
    }

    @Test
    void retirement_shouldBeRefused_untilEveryConditionIsMet_thenBeReversible() throws Exception {
        mvc.perform(get(BASE + "/settings/code-sign-in").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.canRetire").value(false))
                .andExpect(jsonPath("$.retired").value(false));
        postJson("/settings/code-sign-in/retire", "{\"confirmation\":\"RETIRE\"}", adminSession)
                .andExpect(status().isUnprocessableEntity());

        // Two admins with passwords who signed in with them; the first admin sets its password too.
        String secondId = invite("second.admin@example.test", "admin", "[]", "[]");
        activate(linkTo("second.admin@example.test", 1), PASSWORD);
        String thirdId = invite("third.admin@example.test", "admin", "[]", "[]");
        activate(linkTo("third.admin@example.test", 1), PASSWORD);
        // Finishing an activation link is not a password sign-in: neither admin counts yet.
        mvc.perform(get(BASE + "/settings/code-sign-in").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.conditions.adminsWithPasswordSignIn").value(0));
        loginOk("second.admin@example.test", PASSWORD);
        loginOk("third.admin@example.test", PASSWORD);
        String adminCookie = loginOk("second.admin@example.test", PASSWORD);
        mvc.perform(post(BASE + "/users/" + accounts.findByEmail(ADMIN).orElseThrow().getId() + "/disable")
                .with(signedIn(adminCookie))).andExpect(status().isOk());

        // Real delivery attested (with evidence) for an invitation and a reset whose links were used.
        String invitationDelivery = JsonPath.read(mvc.perform(get(BASE + "/users/" + secondId + "/emails")
                .with(signedIn(adminCookie))).andReturn().getResponse().getContentAsString(), "$[-1].deliveryId");
        postJson("/email-deliveries/" + invitationDelivery + "/confirm", "{\"evidence\":\"provider log 1234\"}", adminCookie)
                .andExpect(status().isOk());
        // A manual attestation: who, when and the evidence are kept; never a provider "delivered".
        mvc.perform(get(BASE + "/users/" + secondId + "/emails").with(signedIn(adminCookie)))
                .andExpect(jsonPath("$[-1].delivery").value("confirmed_by_admin"))
                .andExpect(jsonPath("$[-1].deliveryConfirmedByEmail").value("second.admin@example.test"))
                .andExpect(jsonPath("$[-1].deliveryEvidence").value("provider log 1234"))
                .andExpect(jsonPath("$[-1].deliveryConfirmedAt").isNotEmpty());
        assertTrue(auditEvents.findAll().stream().anyMatch(e -> e.getAction() == SiteSecurityAuditAction.EMAIL_DELIVERY_CONFIRMED
                && ("manual:INVITATION:" + invitationDelivery).equals(e.getDetail())));
        postJson("/auth/forgot-password", "{\"email\":\"third.admin@example.test\"}", null).andExpect(status().isAccepted());
        String resetToken = TestMail.token(awaitSubject("third.admin@example.test", "Réinitialisation de votre mot de passe Farm Security"));
        postJson("/auth/reset-password", "{\"token\":\"" + resetToken + "\",\"password\":\"" + PASSWORD + "\"}", null)
                .andExpect(status().isNoContent());
        loginOk("third.admin@example.test", PASSWORD);
        String emails = mvc.perform(get(BASE + "/users/" + thirdId + "/emails").with(signedIn(adminCookie)))
                .andReturn().getResponse().getContentAsString();
        List<String> resetIds = JsonPath.read(emails, "$[?(@.kind == 'password_reset')].deliveryId");
        postJson("/email-deliveries/" + resetIds.get(0) + "/confirm", "{\"evidence\":\"provider log 5678\"}", adminCookie)
                .andExpect(status().isOk());

        postJson("/settings/code-sign-in/retire", "{\"confirmation\":\"yes\"}", adminCookie).andExpect(status().isUnprocessableEntity());
        postJson("/settings/code-sign-in/retire", "{\"confirmation\":\"RETIRE\"}", adminCookie)
                .andExpect(status().isOk()).andExpect(jsonPath("$.retired").value(true));

        // Retired: a legacy account without password gets no code; sessions were not revoked.
        TestSessions.open(accounts, sessions, "late.legacy@example.test", SiteSecurityRole.READ);
        TestMail.clear(mail);
        postJson("/auth/request-code", "{\"email\":\"late.legacy@example.test\"}", null).andExpect(status().isAccepted());
        Thread.sleep(400);
        assertTrue(TestMail.to(mail, "late.legacy@example.test").isEmpty());
        session(adminCookie).andExpect(status().isOk());

        postJson("/settings/code-sign-in/restore", "{}", adminCookie).andExpect(status().isOk())
                .andExpect(jsonPath("$.retired").value(false));
        assertTrue(auditEvents.findAll().stream().anyMatch(e -> e.getAction() == SiteSecurityAuditAction.CODE_SIGN_IN_RETIRED));
        assertTrue(auditEvents.findAll().stream().anyMatch(e -> e.getAction() == SiteSecurityAuditAction.CODE_SIGN_IN_RESTORED));
    }

    @Test
    void deliveryAttestation_shouldBeRefusedForAnEmailNeverSubmitted_andNeverCountAsRealDelivery() throws Exception {
        String userId = invite("never.sent@example.test", "user", "[\"view_zones\"]", "[]");
        String link = linkTo("never.sent@example.test", 1);
        String deliveryId = deliveries.findByAccountIdOrderByCreatedAtDesc(userId).get(0).getId();
        for (String notSent : List.of("FAILED", "MAIL_DISABLED", "LOCAL_CONSOLE", "QUEUED", "RETRY_SCHEDULED")) {
            var row = deliveries.findById(deliveryId).orElseThrow();
            row.setSubmissionStatus(notSent);
            deliveries.save(row);
            postJson("/email-deliveries/" + deliveryId + "/confirm", "{\"evidence\":\"told me\"}", adminSession)
                    .andExpect(status().isUnprocessableEntity());
            assertEquals("UNKNOWN", deliveries.findById(deliveryId).orElseThrow().getDeliveryStatus(), notSent);
        }
        // Even a row attested earlier (before this rule) does not count unless it was submitted.
        activate(link, PASSWORD);
        var row = deliveries.findById(deliveryId).orElseThrow();
        row.setSubmissionStatus("LOCAL_CONSOLE");
        row.setDeliveryStatus("CONFIRMED_BY_ADMIN");
        deliveries.save(row);
        mvc.perform(get(BASE + "/settings/code-sign-in").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.conditions.realDeliveryVerified").value(false));
        assertFalse(auditEvents.findAll().stream().anyMatch(e -> e.getAction() == SiteSecurityAuditAction.EMAIL_DELIVERY_CONFIRMED));
    }

    // ------------------------------------------------------------------ admin rules

    @Test
    void disable_shouldEndSessionsAndLinks_andEnableShouldRestore() throws Exception {
        String id = inviteUser();
        String cookie = activate(linkTo(USER, 1), PASSWORD);
        postJson("/auth/forgot-password", "{\"email\":\"" + USER + "\"}", null).andExpect(status().isAccepted());
        String resetToken = TestMail.token(awaitSubject(USER, "Réinitialisation de votre mot de passe Farm Security"));

        mvc.perform(post(BASE + "/users/" + id + "/disable").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.status").value("disabled"));
        session(cookie).andExpect(status().isUnauthorized());
        postJson("/auth/reset-password", "{\"token\":\"" + resetToken + "\",\"password\":\"" + OTHER_PASSWORD + "\"}", null)
                .andExpect(status().isBadRequest());

        mvc.perform(post(BASE + "/users/" + id + "/enable").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.status").value("active"));
        loginOk(USER, PASSWORD);
    }

    @Test
    void endOfValidity_shouldEndAccess() throws Exception {
        String id = inviteUser();
        String cookie = activate(linkTo(USER, 1), PASSWORD);
        OperatorEntity account = accounts.findById(id).orElseThrow();
        account.setValidUntil(LocalDate.now(ZoneOffset.UTC).minusDays(1));
        accounts.save(account);

        session(cookie).andExpect(status().isUnauthorized());
        login(USER, PASSWORD, null).andExpect(status().isUnauthorized());
        assertEquals("disabled", statusOf(id));
    }

    @Test
    void lastAdmin_cannotBeDemotedDisabledOrGivenAnEndDate() throws Exception {
        String adminId = accounts.findByEmail(ADMIN).orElseThrow().getId();
        String body = "{\"fullName\":\"First Admin\",\"category\":\"security\",\"role\":\"%s\"%s}";

        mvc.perform(patch(BASE + "/users/" + adminId).with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(body, "user", ""))).andExpect(status().isUnprocessableEntity());
        mvc.perform(patch(BASE + "/users/" + adminId).with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(body, "admin", ",\"validUntil\":\"" + LocalDate.now(ZoneOffset.UTC).plusDays(5) + "\"")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post(BASE + "/users/" + adminId + "/disable").with(signedIn(adminSession)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invite_shouldRefuseMismatchedOrDuplicateEmails_andUngrantablePermissions() throws Exception {
        String base = "{\"fullName\":\"X Y\",\"email\":\"x@example.test\",\"emailConfirmation\":\"%s\",\"role\":\"user\","
                + "\"category\":\"worker\",\"permissions\":%s,\"zoneIds\":%s}";
        mvc.perform(post(BASE + "/users").with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(base, "y@example.test", "[]", "[]"))).andExpect(status().isUnprocessableEntity());
        mvc.perform(post(BASE + "/users").with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(base, "x@example.test", "[\"legacy_read_all\"]", "[]"))).andExpect(status().isUnprocessableEntity());
        mvc.perform(post(BASE + "/users").with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(base, "x@example.test", "[]", "[\"moon\"]"))).andExpect(status().isUnprocessableEntity());
        mvc.perform(post(BASE + "/users").with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(base, "x@example.test", "[]", "[]"))).andExpect(status().isCreated());
        mvc.perform(post(BASE + "/users").with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(base, "X@Example.test", "[]", "[]"))).andExpect(status().isConflict());
    }

    @Test
    void summaryAndHistory_shouldTrackEveryInvitation() throws Exception {
        String pending = invite("pending.person@example.test", "user", "[]", "[]");
        String revoked = invite("revoked.person@example.test", "user", "[]", "[]");
        inviteUser();
        activate(linkTo(USER, 1), PASSWORD);
        mvc.perform(post(BASE + "/users/" + revoked + "/invitation/revoke").with(signedIn(adminSession))).andExpect(status().isOk());

        mvc.perform(get(BASE + "/users/summary").with(signedIn(adminSession)))
                .andExpect(jsonPath("$.pending").value(1))
                .andExpect(jsonPath("$.revoked").value(1))
                .andExpect(jsonPath("$.active").value(2));

        String userId = accounts.findByEmail(USER).orElseThrow().getId();
        String history = mvc.perform(get(BASE + "/users/" + userId + "/history").with(signedIn(adminSession)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> actions = JsonPath.read(history, "$[*].action");
        List<String> kinds = JsonPath.read(history, "$[*].actor.kind");
        assertTrue(actions.contains("account_invited") && actions.contains("account_activated"), actions.toString());
        assertTrue(kinds.contains("admin") && kinds.contains("self"), kinds.toString());
        assertEquals("pending", statusOf(pending));
    }

    @Test
    void users_shouldBeAdminOnly() throws Exception {
        inviteUser();
        String cookie = activate(linkTo(USER, 1), PASSWORD);

        mvc.perform(get(BASE + "/users").cookie(new Cookie(SessionService.COOKIE_NAME, cookie))).andExpect(status().isForbidden());
        postJson("/users/x/disable", "{}", cookie).andExpect(status().isForbidden());
        mvc.perform(get(BASE + "/settings/code-sign-in").cookie(new Cookie(SessionService.COOKIE_NAME, cookie)))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ review fixes

    @Test
    void legacyReadOnlyAccess_canBeKeptOnEdit_butNeverGiven() throws Exception {
        TestSessions.open(accounts, sessions, "legacy.reader@example.test", SiteSecurityRole.READ);
        String legacyId = accounts.findByEmail("legacy.reader@example.test").orElseThrow().getId();
        String keep = "{\"fullName\":\"Legacy Reader\",\"category\":\"worker\",\"role\":\"user\",\"permissions\":[\"legacy_read_all\"],\"zoneIds\":[]}";
        mvc.perform(patch(BASE + "/users/" + legacyId).with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(keep)).andExpect(status().isOk()).andExpect(jsonPath("$.permissions[0]").value("legacy_read_all"));

        String newId = invite("new.reader@example.test", "user", "[]", "[]");
        mvc.perform(patch(BASE + "/users/" + newId).with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                .content(keep)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void partialPatch_shouldKeepPermissionsAndZones() throws Exception {
        String id = inviteUser();

        mvc.perform(patch(BASE + "/users/" + id).with(signedIn(adminSession)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ada Renamed\",\"category\":\"worker\",\"role\":\"user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions[0]").value("view_zones"))
                .andExpect(jsonPath("$.zoneIds[0]").value("open"));
    }

    @Test
    void wrongCurrentPasswords_shouldPauseChangingThePassword_butNeverSignIn() throws Exception {
        inviteUser();
        String cookie = activate(linkTo(USER, 1), PASSWORD);

        for (int i = 0; i < 10; i++) {
            postJson("/account/password", "{\"currentPassword\":\"wrong guess number " + i + "\",\"newPassword\":\"" + OTHER_PASSWORD + "\"}", cookie)
                    .andExpect(status().isBadRequest());
        }
        String accountId = accounts.findByEmail(USER).orElseThrow().getId();
        assertEquals(10, failures.countPasswordFailuresSince(accountId, Instant.now().minusSeconds(900)));
        postJson("/account/password", "{\"currentPassword\":\"" + PASSWORD + "\",\"newPassword\":\"" + OTHER_PASSWORD + "\"}", cookie)
                .andExpect(status().isBadRequest());
        // A stolen session cannot lock the owner out of signing in.
        login(USER, PASSWORD, null).andExpect(status().isOk());
    }

    @Test
    void passwordSetupLink_shouldEndTheLegacyCodeSessions() throws Exception {
        String legacySession = TestSessions.open(accounts, sessions, "legacy.user@example.test", SiteSecurityRole.READ);
        String id = accounts.findByEmail("legacy.user@example.test").orElseThrow().getId();

        mvc.perform(post(BASE + "/users/" + id + "/invitation").with(signedIn(adminSession))).andExpect(status().isOk());
        MimeMessage setup = TestMail.await(mail, "legacy.user@example.test", 1);
        assertEquals("Définissez votre mot de passe Farm Security", TestMail.subject(setup));
        String fresh = activate(TestMail.token(setup), PASSWORD);

        session(legacySession).andExpect(status().isUnauthorized());
        session(fresh).andExpect(status().isOk()).andExpect(jsonPath("$.passwordSet").value(true))
                .andExpect(jsonPath("$.permissions[0]").value("legacy_read_all"));
    }

    // ------------------------------------------------------------------ privacy

    @Test
    void moduleLogs_shouldHoldNoPasswordTokenOrEmail(CapturedOutput output) throws Exception {
        inviteUser();
        String token = linkTo(USER, 1);
        String cookie = activate(token, PASSWORD);
        login(USER, OTHER_PASSWORD, null);
        postJson("/auth/forgot-password", "{\"email\":\"" + USER + "\"}", null);
        String resetToken = TestMail.token(awaitSubject(USER, "Réinitialisation de votre mot de passe Farm Security"));

        String moduleLines = output.getAll().lines().filter(line -> line.contains("c.i.s.m.s."))
                .reduce("", (a, b) -> a + b + "\n");
        for (String secret : List.of(PASSWORD, OTHER_PASSWORD, token, resetToken, cookie, USER)) {
            assertFalse(moduleLines.contains(secret), "module logs leak a secret or an email");
        }
        UUID.fromString(accounts.findByEmail(USER).orElseThrow().getId());
    }
}

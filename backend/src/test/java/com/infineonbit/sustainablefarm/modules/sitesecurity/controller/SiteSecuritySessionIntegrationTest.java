package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorSessionEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SessionService;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions.signedIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-session;DB_CLOSE_DELAY=-1",
        "app.mail.enabled=true",
        "SITESECURITY_OTP_SECRET=otp-secret-for-session-tests-0123456789abcd",
        "SITESECURITY_GATE_TOKEN=" + SiteSecuritySessionIntegrationTest.GATE_TOKEN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SiteSecuritySessionIntegrationTest {

    static final String GATE_TOKEN = "gate-token-for-session-tests-0123456789abcd";

    private static final String BASE = "/api/v1/sitesecurity";
    private static final String EMAIL = "admin@example.test";
    private static final Pattern CODE_IN_EMAIL = Pattern.compile("<strong>(\\d{6})</strong>");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository operators;

    @Autowired
    private OperatorSessionRepository sessions;

    @Autowired
    private LoginCodeRepository loginCodes;

    @MockitoBean
    private NotificationService notifications;

    private OperatorEntity admin;

    @BeforeEach
    void resetData() {
        reset(notifications);
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(true);
        sessions.deleteAll();
        loginCodes.deleteAll();
        operators.deleteAll();
        admin = operators.save(OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(EMAIL)
                .role(SiteSecurityRole.ADMIN)
                .active(true)
                .createdBy("test")
                .createdAt(Instant.now())
                .build());
    }

    private String requestAndReadCode() throws Exception {
        clearInvocations(notifications);
        mvc.perform(post(BASE + "/auth/request-code")
                        .header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(5000)).send(eq(EMAIL), anyString(), body.capture());
        Matcher matcher = CODE_IN_EMAIL.matcher(body.getValue());
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private ResultActions verifyCode(String code) throws Exception {
        return mvc.perform(post(BASE + "/auth/verify")
                .header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"code\":\"" + code + "\"}"));
    }

    private ResultActions logout(String session) throws Exception {
        var request = post(BASE + "/auth/logout").header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN);
        if (session != null) {
            request.cookie(new Cookie(SessionService.COOKIE_NAME, session));
        }
        return mvc.perform(request);
    }

    private static String cookieValue(String setCookie) {
        String pair = setCookie.split(";", 2)[0];
        assertTrue(pair.startsWith(SessionService.COOKIE_NAME + "="), setCookie);
        return pair.substring(SessionService.COOKIE_NAME.length() + 1);
    }

    private String signIn() throws Exception {
        MvcResult result = verifyCode(requestAndReadCode()).andExpect(status().isOk()).andReturn();
        return cookieValue(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
    }

    // --- sign-in ---

    @Test
    void rightCode_shouldOpenASessionInAnHttpOnlyStrictCookie() throws Exception {
        MvcResult result = verifyCode(requestAndReadCode())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andReturn();

        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("HttpOnly"), setCookie);
        assertTrue(setCookie.contains("SameSite=Strict"), setCookie);
        assertTrue(setCookie.contains("Secure"), setCookie);
        assertTrue(setCookie.contains("Path=/api/v1/sitesecurity"), setCookie);
        assertTrue(setCookie.contains("Max-Age=28800"), setCookie);

        String session = cookieValue(setCookie);
        assertTrue(session.length() >= 43, "at least 256 bits");
        mvc.perform(get(BASE + "/session").with(signedIn(session)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void session_shouldBeStoredAsAHashOnly_andRecordTheSignIn() throws Exception {
        String session = signIn();

        List<OperatorSessionEntity> stored = sessions.findAll();
        assertEquals(1, stored.size());
        assertEquals(SessionService.hash(session), stored.get(0).getTokenHash());
        assertFalse(stored.get(0).getTokenHash().contains(session));
        assertEquals(admin.getId(), stored.get(0).getOperatorId());
        assertNotNull(operators.findById(admin.getId()).orElseThrow().getLastLoginAt());
    }

    @Test
    void session_shouldLastEightHours_andNotBeExtendedByUse() throws Exception {
        String session = signIn();
        OperatorSessionEntity before = sessions.findAll().get(0);
        assertEquals(Duration.ofHours(8), Duration.between(before.getCreatedAt(), before.getExpiresAt()));

        for (int i = 0; i < 3; i++) {
            mvc.perform(get(BASE + "/zones").with(signedIn(session))).andExpect(status().isOk());
        }

        assertEquals(before.getExpiresAt(), sessions.findAll().get(0).getExpiresAt());
    }

    @Test
    void wrongCode_shouldNotSetACookie() throws Exception {
        String code = requestAndReadCode();
        String wrong = code.equals("000000") ? "000001" : "000000";

        MvcResult result = verifyCode(wrong).andExpect(status().isUnauthorized()).andReturn();

        assertNull(result.getResponse().getHeader(HttpHeaders.SET_COOKIE));
        assertEquals(0, sessions.count());
    }

    @Test
    void eachSignIn_shouldOpenItsOwnSession() throws Exception {
        String first = signIn();
        loginCodes.deleteAll(); // lets the next code go out without waiting a minute
        String second = signIn();

        assertFalse(first.equals(second));
        mvc.perform(get(BASE + "/session").with(signedIn(first))).andExpect(status().isOk());
        mvc.perform(get(BASE + "/session").with(signedIn(second))).andExpect(status().isOk());
    }

    // --- end of a session ---

    @Test
    void logout_shouldCloseTheSession_andClearTheCookie() throws Exception {
        String session = signIn();

        MvcResult result = logout(session).andExpect(status().isNoContent()).andReturn();

        String cleared = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertTrue(cleared.contains("Max-Age=0"), cleared);
        assertTrue(cleared.contains("Path=/api/v1/sitesecurity"), cleared);
        mvc.perform(get(BASE + "/session").with(signedIn(session))).andExpect(status().isUnauthorized());
        assertNotNull(sessions.findAll().get(0).getRevokedAt());
    }

    @Test
    void logout_shouldSucceed_withoutOrWithAStaleCookie() throws Exception {
        logout(null).andExpect(status().isNoContent());
        logout("not-a-session").andExpect(status().isNoContent());
    }

    @Test
    void logout_shouldOnlyCloseItsOwnSession() throws Exception {
        String kept = TestSessions.openFor(sessions, admin.getId(), Duration.ofHours(8));
        String closed = TestSessions.openFor(sessions, admin.getId(), Duration.ofHours(8));

        logout(closed).andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/session").with(signedIn(kept))).andExpect(status().isOk());
    }

    @Test
    void expiredSession_shouldBeRefused() throws Exception {
        String expired = TestSessions.openFor(sessions, admin.getId(), Duration.ofSeconds(-1));

        mvc.perform(get(BASE + "/session").with(signedIn(expired))).andExpect(status().isUnauthorized());
    }

    @Test
    void session_shouldBeRefused_whenTheOperatorIsDisabledOrRemoved() throws Exception {
        String session = TestSessions.openFor(sessions, admin.getId(), Duration.ofHours(8));
        admin.setActive(false);
        operators.save(admin);

        mvc.perform(get(BASE + "/session").with(signedIn(session))).andExpect(status().isUnauthorized());

        operators.deleteAll();
        mvc.perform(get(BASE + "/session").with(signedIn(session))).andExpect(status().isUnauthorized());
    }

    // --- cross-site protection ---

    @Test
    void writes_shouldRequireTheAppOrigin_evenWithAValidSession() throws Exception {
        String session = TestSessions.openFor(sessions, admin.getId(), Duration.ofHours(8));
        Cookie cookie = new Cookie(SessionService.COOKIE_NAME, session);

        mvc.perform(post(BASE + "/credentials/u1/revoke").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This request must come from the Site Security app."));
        mvc.perform(post(BASE + "/credentials/u1/revoke").cookie(cookie)
                        .header(HttpHeaders.ORIGIN, "https://evil.example"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/credentials/u1/revoke").cookie(cookie)
                        .header(HttpHeaders.ORIGIN, "null"))
                .andExpect(status().isForbidden());
        // A Bearer header does not lift the check when a session cookie is sent.
        mvc.perform(post(BASE + "/credentials/u1/revoke").cookie(cookie)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer x"))
                .andExpect(status().isForbidden());
        // With the app's origin, the request reaches the controller (unknown credential).
        mvc.perform(post(BASE + "/credentials/u1/revoke").with(signedIn(session)))
                .andExpect(status().isNotFound());
    }

    @Test
    void reads_shouldNotNeedAnOrigin() throws Exception {
        String session = TestSessions.openFor(sessions, admin.getId(), Duration.ofHours(8));

        mvc.perform(get(BASE + "/zones").cookie(new Cookie(SessionService.COOKIE_NAME, session)))
                .andExpect(status().isOk());
    }

    @Test
    void signInAndSignOut_shouldAlsoRequireTheAppOrigin() throws Exception {
        mvc.perform(post(BASE + "/auth/request-code").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/auth/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"code\":\"123456\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/auth/logout")).andExpect(status().isForbidden());
    }

    // --- gate scanner ---

    @Test
    void gateToken_shouldNotOpenTheOperatorApi() throws Exception {
        for (String path : List.of("/session", "/overview", "/zones", "/credentials", "/logs", "/operators")) {
            mvc.perform(get(BASE + path).header(HttpHeaders.AUTHORIZATION, "Bearer " + GATE_TOKEN))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("The gate token cannot be used here."));
        }
        mvc.perform(post(BASE + "/credentials/u1/revoke").header(HttpHeaders.AUTHORIZATION, "Bearer " + GATE_TOKEN))
                .andExpect(status().isForbidden());
    }

    // --- logs ---

    @Test
    void logs_shouldNeverContainTheCookieValueOrTheEmail(CapturedOutput output) throws Exception {
        String session = signIn();
        mvc.perform(get(BASE + "/zones").with(signedIn(session))).andExpect(status().isOk());
        logout(session).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/zones").with(signedIn(session))).andExpect(status().isUnauthorized());

        String module = output.getAll().lines()
                .filter(line -> line.contains("m.s.service") || line.contains("m.s.config")
                        || line.contains("modules.sitesecurity"))
                .reduce("", (a, b) -> a + b + "\n");
        assertTrue(module.contains("signed in"), "the sign-in is logged");
        assertFalse(output.getAll().contains(session), "cookie value in logs");
        assertFalse(module.contains(EMAIL), "email in module logs");
    }
}

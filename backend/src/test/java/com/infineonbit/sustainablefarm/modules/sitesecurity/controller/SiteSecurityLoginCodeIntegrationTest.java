package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LoginCodeEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import java.net.URI;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
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
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-otp;DB_CLOSE_DELAY=-1",
        "app.mail.enabled=true",
        "SITESECURITY_OTP_SECRET=otp-secret-for-integration-tests-0123456789",
        "SITESECURITY_READ_TOKEN=read-token-for-otp-tests-0123456789abcdef",
        "SITESECURITY_ADMIN_TOKEN=admin-token-for-otp-tests-0123456789abcde"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SiteSecurityLoginCodeIntegrationTest {

    private static final String AUTH = "/api/v1/sitesecurity/auth";
    private static final String EMAIL = "operator@example.test";
    private static final Pattern CODE_IN_EMAIL = Pattern.compile("<strong>(\\d{6})</strong>");
    private static final long WAIT_MS = 5000;
    private static final long QUIET_MS = 400;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository operators;

    @Autowired
    private LoginCodeRepository loginCodes;

    @MockitoBean
    private NotificationService notifications;

    private OperatorEntity operator;

    @BeforeEach
    void resetData() {
        reset(notifications);
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(true);
        loginCodes.deleteAll();
        operators.deleteAll();
        operator = operator(EMAIL, true);
    }

    private OperatorEntity operator(String email, boolean active) {
        return operators.save(OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .role(SiteSecurityRole.READ)
                .active(active)
                .createdBy("test")
                .createdAt(Instant.now())
                .build());
    }

    private ResultActions request(String email) throws Exception {
        return mvc.perform(post(AUTH + "/request-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions verifyCode(String email, String code) throws Exception {
        return mvc.perform(post(AUTH + "/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"code\":\"" + code + "\"}"));
    }

    private String requestAndReadCode() throws Exception {
        clearInvocations(notifications);
        request(EMAIL).andExpect(status().isAccepted());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(WAIT_MS)).send(eq(EMAIL), anyString(), body.capture());
        Matcher matcher = CODE_IN_EMAIL.matcher(body.getValue());
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private String wrongCode(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }

    private LoginCodeEntity onlyCode() {
        List<LoginCodeEntity> all = loginCodes.findAll();
        assertEquals(1, all.size());
        return all.get(0);
    }

    private void ageCode(LoginCodeEntity code, Duration age) {
        code.setCreatedAt(Instant.now().minus(age));
        loginCodes.save(code);
    }

    private void assertNoEmailSent() {
        verify(notifications, after(QUIET_MS).never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void endpoints_shouldNeedNoToken_whileTheRestOfTheApiStaysLocked() throws Exception {
        request("nobody@example.test").andExpect(status().isAccepted());
        verifyCode("nobody@example.test", "123456").andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/sitesecurity/zones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/sitesecurity/operators")).andExpect(status().isUnauthorized());
        mvc.perform(get(URI.create(AUTH + "/verify/../zones"))).andExpect(status().isUnauthorized());
    }

    @Test
    void request_shouldAnswerTheSame_whateverTheEmail() throws Exception {
        operator("disabled@example.test", false);

        String known = request(EMAIL).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String unknown = request("nobody@example.test").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String disabled = request("disabled@example.test").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String limited = request(EMAIL).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertEquals(known, unknown);
        assertEquals(known, disabled);
        assertEquals(known, limited);
    }

    @Test
    void request_shouldSendNothing_forAnUnknownOrDisabledEmail() throws Exception {
        operator("disabled@example.test", false);

        request("nobody@example.test").andExpect(status().isAccepted());
        request("disabled@example.test").andExpect(status().isAccepted());

        assertNoEmailSent();
        assertEquals(0, loginCodes.count());
    }

    @Test
    void request_shouldEmailASixDigitCode_andStoreOnlyItsHash() throws Exception {
        String code = requestAndReadCode();

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(notifications).send(anyString(), subject.capture(), anyString());
        assertFalse(subject.getValue().contains(code));

        LoginCodeEntity stored = onlyCode();
        assertEquals(64, stored.getCodeHash().length());
        assertFalse(stored.getCodeHash().contains(code));
        assertEquals(0, stored.getAttempts());
        long minutes = Duration.between(stored.getCreatedAt(), stored.getExpiresAt()).toMinutes();
        assertEquals(5, minutes);
    }

    @Test
    void request_shouldAcceptTheEmailInAnyCase() throws Exception {
        request("OPERATOR@Example.TEST").andExpect(status().isAccepted());

        verify(notifications, timeout(WAIT_MS)).send(eq(EMAIL), anyString(), anyString());
    }

    @Test
    void sameCode_shouldNotGiveTheSameHashTwice() throws Exception {
        requestAndReadCode();
        String firstHash = onlyCode().getCodeHash();
        ageCode(onlyCode(), Duration.ofMinutes(2));
        requestAndReadCode();

        assertTrue(loginCodes.findAll().stream().map(LoginCodeEntity::getCodeHash).distinct().count() == 2);
        assertNotNull(firstHash);
    }

    @Test
    void verify_shouldAcceptTheCodeOnce() throws Exception {
        String code = requestAndReadCode();

        verifyCode(EMAIL, code).andExpect(status().isOk());
        assertNotNull(onlyCode().getConsumedAt());

        verifyCode(EMAIL, code).andExpect(status().isUnauthorized());
    }

    @Test
    void verify_shouldAnswerTheSame_forEveryFailure() throws Exception {
        String code = requestAndReadCode();

        String wrong = refused(EMAIL, wrongCode(code));
        assertEquals(wrong, refused("nobody@example.test", code));

        LoginCodeEntity stored = onlyCode();
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        loginCodes.save(stored);
        assertEquals(wrong, refused(EMAIL, code));
    }

    private String refused(String email, String code) throws Exception {
        MvcResult result = verifyCode(email, code).andExpect(status().isUnauthorized()).andReturn();
        return result.getResponse().getContentAsString();
    }

    @Test
    void verify_shouldAcceptTheCode_onTheFifthAttempt() throws Exception {
        String code = requestAndReadCode();

        for (int attempt = 0; attempt < 4; attempt++) {
            verifyCode(EMAIL, wrongCode(code)).andExpect(status().isUnauthorized());
        }
        assertEquals(4, onlyCode().getAttempts());

        verifyCode(EMAIL, code).andExpect(status().isOk());
    }

    @Test
    void verify_shouldRefuseTheRightCode_afterFiveWrongAttempts() throws Exception {
        String code = requestAndReadCode();

        for (int attempt = 0; attempt < 5; attempt++) {
            verifyCode(EMAIL, wrongCode(code)).andExpect(status().isUnauthorized());
        }

        verifyCode(EMAIL, code).andExpect(status().isUnauthorized());
        assertEquals(5, onlyCode().getAttempts());
    }

    @Test
    void verify_shouldRefuseAnExpiredCode() throws Exception {
        String code = requestAndReadCode();
        LoginCodeEntity stored = onlyCode();
        stored.setExpiresAt(Instant.now().minusSeconds(1));
        loginCodes.save(stored);

        verifyCode(EMAIL, code).andExpect(status().isUnauthorized());
    }

    @Test
    void verify_shouldRefuseTheCode_ofAnOperatorDisabledInTheMeantime() throws Exception {
        String code = requestAndReadCode();
        operator.setActive(false);
        operators.save(operator);

        verifyCode(EMAIL, code).andExpect(status().isUnauthorized());
    }

    @Test
    void verify_shouldRefuseAnotherOperatorsCode() throws Exception {
        String code = requestAndReadCode();
        operator("other@example.test", true);

        verifyCode("other@example.test", code).andExpect(status().isUnauthorized());
    }

    @Test
    void verify_shouldRejectAMalformedRequest() throws Exception {
        verifyCode(EMAIL, "12345").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists());
        verifyCode(EMAIL, "12345a").andExpect(status().isBadRequest());
        verifyCode("not-an-email", "123456").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void newCode_shouldReplaceThePreviousOne() throws Exception {
        String first = requestAndReadCode();
        ageCode(onlyCode(), Duration.ofMinutes(2));
        String second = requestAndReadCode();

        if (!first.equals(second)) {
            verifyCode(EMAIL, first).andExpect(status().isUnauthorized());
        }
        verifyCode(EMAIL, second).andExpect(status().isOk());
    }

    @Test
    void request_shouldSendOneCodePerMinute() throws Exception {
        requestAndReadCode();
        clearInvocations(notifications);

        request(EMAIL).andExpect(status().isAccepted());

        assertNoEmailSent();
        assertEquals(1, loginCodes.count());
    }

    @Test
    void limitedRequest_shouldKeepTheCurrentCodeValid() throws Exception {
        String code = requestAndReadCode();
        clearInvocations(notifications);

        request(EMAIL).andExpect(status().isAccepted());
        assertNoEmailSent();

        verifyCode(EMAIL, code).andExpect(status().isOk());
    }

    private void pastCode(Duration age) {
        Instant createdAt = Instant.now().minus(age);
        loginCodes.save(LoginCodeEntity.builder()
                .id(UUID.randomUUID().toString())
                .operatorId(operator.getId())
                .codeHash("0".repeat(64))
                .expiresAt(createdAt.plusSeconds(300))
                .consumedAt(createdAt)
                .createdAt(createdAt)
                .build());
    }

    @Test
    void request_shouldSendFiveCodesPerHour() throws Exception {
        for (int minutes = 10; minutes <= 50; minutes += 10) {
            pastCode(Duration.ofMinutes(minutes));
        }

        request(EMAIL).andExpect(status().isAccepted());

        assertNoEmailSent();
        assertEquals(5, loginCodes.count());
    }

    @Test
    void request_shouldSendAgain_onceOldCodesLeaveTheHour() throws Exception {
        for (int minutes = 61; minutes <= 65; minutes++) {
            pastCode(Duration.ofMinutes(minutes));
        }

        requestAndReadCode();

        assertEquals(1, loginCodes.count());
    }

    @Test
    void logs_shouldContainNeitherTheEmailNorTheCode(CapturedOutput output) throws Exception {
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(false);
        String code = requestAndReadCode();
        verifyCode(EMAIL, wrongCode(code)).andExpect(status().isUnauthorized());
        verifyCode(EMAIL, code).andExpect(status().isOk());

        ageCode(onlyCode(), Duration.ofSeconds(1));
        clearInvocations(notifications);
        request(EMAIL).andExpect(status().isAccepted());
        assertNoEmailSent();

        assertTrue(output.getAll().contains(operator.getId()));
        assertFalse(output.getAll().contains(EMAIL));
        assertFalse(output.getAll().contains(code));
        assertNotEquals(-1, output.getAll().indexOf("Sign-in code"));
    }
}

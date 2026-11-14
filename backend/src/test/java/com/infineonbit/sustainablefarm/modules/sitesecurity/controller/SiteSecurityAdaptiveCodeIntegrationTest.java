package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.FailedSignInEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LoginCodeEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.FailedSignInRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LoginCodeRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LoginCodeService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.LoginCodeTestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Adaptive codes: after 10 wrong codes in 24 hours, new codes have 10 digits
 * instead of 6, until the rolling count falls below 10 again.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-adaptive;DB_CLOSE_DELAY=-1",
        "app.mail.enabled=true",
        "SITESECURITY_OTP_SECRET=otp-secret-for-adaptive-tests-0123456789abcd"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SiteSecurityAdaptiveCodeIntegrationTest {

    private static final String AUTH = "/api/v1/sitesecurity/auth";
    private static final String EMAIL = "adaptive@example.test";
    private static final Pattern CODE_IN_EMAIL = Pattern.compile("<strong>(\\d+)</strong>");
    private static final long WAIT_MS = 5000;
    private static final long QUIET_MS = 400;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository operators;

    @Autowired
    private LoginCodeRepository loginCodes;

    @Autowired
    private FailedSignInRepository failedSignIns;

    @Autowired
    private LoginCodeService loginCodeService;

    @MockitoBean
    private NotificationService notifications;

    private OperatorEntity operator;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier(com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors.PUBLIC_REQUESTS)
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor publicRequests;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier(com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityExecutors.ACCOUNT_EMAILS)
    private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor accountEmails;

    @BeforeEach
    void resetData() {
        com.infineonbit.sustainablefarm.modules.sitesecurity.TestAsync.drain(publicRequests, accountEmails);
        reset(notifications);
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(true);
        failedSignIns.deleteAll();
        loginCodes.deleteAll();
        operators.deleteAll();
        operator = operators.save(OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(EMAIL)
                .role(SiteSecurityRole.READ)
                .active(true)
                .createdBy("test")
                .createdAt(Instant.now())
                .build());
    }

    // --- helpers ---

    private ResultActions request() throws Exception {
        return mvc.perform(post(AUTH + "/request-code")
                .header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\"}"));
    }

    private ResultActions verifyCode(String code) throws Exception {
        return mvc.perform(post(AUTH + "/verify")
                .header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"code\":\"" + code + "\"}"));
    }

    /** Requests a code and returns the digits from the email, whatever their number. */
    private String requestAndReadCode() throws Exception {
        clearInvocations(notifications);
        request().andExpect(status().isAccepted());
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(WAIT_MS)).send(eq(EMAIL), anyString(), body.capture());
        Matcher matcher = CODE_IN_EMAIL.matcher(body.getValue());
        assertTrue(matcher.find(), body.getValue());
        return matcher.group(1);
    }

    /** Stores wrong codes as if they had been tried that long ago. */
    private void pastWrongCodes(int count, Duration ago) {
        for (int i = 0; i < count; i++) {
            failedSignIns.save(FailedSignInEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .operatorId(operator.getId())
                    .occurredAt(Instant.now().minus(ago))
                    .build());
        }
    }

    private static String wrongCode(String code) {
        char last = code.charAt(code.length() - 1);
        return code.substring(0, code.length() - 1) + (last == '0' ? '1' : '0');
    }

    /** Closes the current code and moves it out of the one-minute delay, so that a new one can be sent. */
    private void endCurrentCode() {
        for (LoginCodeEntity code : loginCodes.findAll()) {
            code.setCreatedAt(Instant.now().minus(Duration.ofMinutes(6)));
            code.setExpiresAt(Instant.now().minusSeconds(1));
            loginCodes.save(code);
        }
    }

    /** Stores a usable code with digits chosen by the test. */
    private void storeCode(String digits) {
        String id = UUID.randomUUID().toString();
        loginCodes.save(LoginCodeEntity.builder()
                .id(id)
                .operatorId(operator.getId())
                .codeHash(LoginCodeTestSupport.hash(loginCodeService, id, digits))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                .createdAt(Instant.now())
                .build());
    }

    // --- thresholds ---

    @Test
    void codes_shouldHaveSixDigits_withNineWrongCodesInTheLastDay() throws Exception {
        pastWrongCodes(9, Duration.ofHours(1));

        assertEquals(6, requestAndReadCode().length());
    }

    @Test
    void codes_shouldHaveTenDigits_fromTheTenthWrongCode() throws Exception {
        pastWrongCodes(10, Duration.ofHours(1));

        String code = requestAndReadCode();

        assertEquals(10, code.length());
        verifyCode(code).andExpect(status().isOk());
    }

    @Test
    void wrongCodes_olderThan24Hours_shouldNotCount() throws Exception {
        pastWrongCodes(10, Duration.ofHours(24).plusMinutes(1));

        assertEquals(6, requestAndReadCode().length());
        // They are removed when a code is issued.
        assertEquals(0, failedSignIns.count());
    }

    @Test
    void codes_shouldReturnToSixDigits_onceTheRollingCountFallsBelowTen() throws Exception {
        pastWrongCodes(9, Duration.ofHours(2));
        pastWrongCodes(1, Duration.ofHours(23).plusMinutes(59));
        assertEquals(10, requestAndReadCode().length());

        // A day later for that oldest wrong code: 9 are left in the window.
        failedSignIns.findAll().stream()
                .filter(f -> f.getOccurredAt().isBefore(Instant.now().minus(Duration.ofHours(23))))
                .forEach(f -> failedSignIns.delete(f));
        pastWrongCodes(1, Duration.ofHours(24).plusMinutes(1));
        endCurrentCode();

        assertEquals(6, requestAndReadCode().length());
    }

    @Test
    void wrongCodesTypedNow_shouldSwitchTheNextCodeToTenDigits() throws Exception {
        for (int round = 0; round < 2; round++) {
            String code = requestAndReadCode();
            assertEquals(6, code.length());
            for (int attempt = 0; attempt < 5; attempt++) {
                verifyCode(wrongCode(code)).andExpect(status().isUnauthorized());
            }
            endCurrentCode();
        }
        assertEquals(10, failedSignIns.count());

        assertEquals(10, requestAndReadCode().length());
    }

    @Test
    void rightCode_shouldRecordNoWrongCode() throws Exception {
        String code = requestAndReadCode();
        verifyCode(wrongCode(code)).andExpect(status().isUnauthorized());
        verifyCode(code).andExpect(status().isOk());

        assertEquals(1, failedSignIns.count());
    }

    @Test
    void usableSixDigitCode_shouldStayValid_whenTheThresholdIsReached() throws Exception {
        pastWrongCodes(9, Duration.ofHours(1));
        String code = requestAndReadCode();
        assertEquals(6, code.length());

        verifyCode(wrongCode(code)).andExpect(status().isUnauthorized());
        assertEquals(10, failedSignIns.count());

        // The threshold applies to new codes only: the current code still works.
        verifyCode(code).andExpect(status().isOk());
    }

    @Test
    void tenDigitCode_shouldStillAllowOnlyFiveAttempts() throws Exception {
        pastWrongCodes(10, Duration.ofHours(1));
        String code = requestAndReadCode();
        assertEquals(10, code.length());

        for (int attempt = 0; attempt < 5; attempt++) {
            verifyCode(wrongCode(code)).andExpect(status().isUnauthorized());
        }

        verifyCode(code).andExpect(status().isUnauthorized());
    }

    // --- formats ---

    @Test
    void codesWithLeadingZeros_shouldBeAccepted_inBothLengths() throws Exception {
        storeCode("0000000042");
        verifyCode("0000000042").andExpect(status().isOk());

        storeCode("000042");
        verifyCode("000042").andExpect(status().isOk());
    }

    @Test
    void codeWithoutItsLeadingZeros_shouldBeRejected() throws Exception {
        storeCode("0000000042");

        verifyCode("42").andExpect(status().isBadRequest());
        verifyCode("000000042").andExpect(status().isBadRequest());
        // The right digits are still accepted afterwards: malformed codes cost no attempt.
        verifyCode("0000000042").andExpect(status().isOk());
    }

    @Test
    void verify_shouldOnlyAcceptSixOrTenDigits() throws Exception {
        for (String malformed : List.of("12345", "1234567", "12345678", "123456789", "12345678901", "12345abcde")) {
            verifyCode(malformed).andExpect(status().isBadRequest());
        }
        assertEquals(0, failedSignIns.count());
    }

    @Test
    void sixDigitGuess_againstATenDigitCode_shouldCountAsAWrongCode() throws Exception {
        storeCode("1234567890");

        verifyCode("123456").andExpect(status().isUnauthorized());

        assertEquals(1, failedSignIns.count());
        assertEquals(1, loginCodes.findAll().get(0).getAttempts());
    }

    @Test
    void email_shouldTellHowManyDigitsTheCodeHas() throws Exception {
        pastWrongCodes(10, Duration.ofHours(1));
        clearInvocations(notifications);
        request().andExpect(status().isAccepted());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(WAIT_MS)).send(eq(EMAIL), anyString(), body.capture());
        assertTrue(body.getValue().contains("Ce code de 10 chiffres"), body.getValue());
    }

    // --- concurrency ---

    @Test
    void parallelWrongCodes_shouldEachBeCounted_upToTheFiveAttemptsOfTheCode() throws Exception {
        String code = requestAndReadCode();

        List<Integer> answers = inParallel(12, () -> verifyCode(wrongCode(code)).andReturn().getResponse().getStatus());

        assertTrue(answers.stream().allMatch(s -> s == 401), answers.toString());
        assertEquals(5, loginCodes.findAll().get(0).getAttempts());
        assertEquals(5, failedSignIns.count());
    }

    @Test
    void parallelRequests_atTheThreshold_shouldIssueOneTenDigitCode() throws Exception {
        pastWrongCodes(10, Duration.ofHours(1));
        clearInvocations(notifications);

        inParallel(12, () -> request().andReturn().getResponse().getStatus());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(WAIT_MS)).send(eq(EMAIL), anyString(), body.capture());
        verify(notifications, after(QUIET_MS).times(1)).send(anyString(), anyString(), anyString());
        Matcher matcher = CODE_IN_EMAIL.matcher(body.getValue());
        assertTrue(matcher.find());
        assertEquals(10, matcher.group(1).length());
        assertEquals(1, loginCodes.count());
    }

    // --- privacy ---

    @Test
    void logs_shouldShowTheSwitch_withoutTheEmailOrTheCode(CapturedOutput output) throws Exception {
        pastWrongCodes(10, Duration.ofHours(1));
        String code = requestAndReadCode();
        verifyCode(wrongCode(code)).andExpect(status().isUnauthorized());

        assertTrue(output.getAll().contains("Operator " + operator.getId()
                + " has 10 wrong sign-in codes in 24 hours: sending a 10-digit code"));
        assertFalse(output.getAll().contains(EMAIL));
        assertFalse(output.getAll().contains(code));
        for (FailedSignInEntity failed : failedSignIns.findAll()) {
            assertFalse(failed.toString().contains(code));
        }
    }

    private static <T> List<T> inParallel(int count, java.util.concurrent.Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(task));
            }
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            pool.shutdown();
        }
    }
}

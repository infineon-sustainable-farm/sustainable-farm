package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialNotificationStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import java.time.Duration;
import java.time.LocalDate;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;
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

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-email;DB_CLOSE_DELAY=-1",
        "SITESECURITY_READ_TOKEN=" + SiteSecurityCredentialEmailIntegrationTest.READ_TOKEN,
        "SITESECURITY_ADMIN_TOKEN=" + SiteSecurityCredentialEmailIntegrationTest.ADMIN_TOKEN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SiteSecurityCredentialEmailIntegrationTest {

    static final String READ_TOKEN = "read-token-for-email-tests-0123456789abcd";
    static final String ADMIN_TOKEN = "admin-token-for-email-tests-0123456789abc";

    private static final String CREDENTIALS = "/api/v1/sitesecurity/credentials";
    private static final String EMAIL = "holder@example.test";
    private static final long WAIT_MS = 5000;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CredentialUserRepository credentials;

    @MockitoBean
    private NotificationService notifications;

    @BeforeEach
    void resetData() {
        reset(notifications);
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(true);
        credentials.deleteAll();
    }

    private String json(String name, String email) {
        String validUntil = LocalDate.now().plusDays(10).toString();
        return "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"role\":\"Tester\","
                + "\"type\":\"Staff\",\"level\":\"controlled\",\"validUntil\":\"" + validUntil + "\"}";
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post(CREDENTIALS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions revoke(String id) throws Exception {
        return mvc.perform(post(CREDENTIALS + "/" + id + "/revoke")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN));
    }

    private CredentialUserEntity created(String name) throws Exception {
        create(json(name, EMAIL)).andExpect(status().isCreated());
        return credentials.findAll().stream().filter(c -> name.equals(c.getName())).findFirst().orElseThrow();
    }

    private void awaitStatus(String id, CredentialNotificationStatus expected) {
        Awaitility.await().atMost(Duration.ofMillis(WAIT_MS)).until(() ->
                credentials.findById(id).orElseThrow().getNotificationStatus() == expected);
    }

    @Test
    void create_shouldEmailTheHolder_andRecordThatItWasSent() throws Exception {
        create(json("Ada Holder", "Holder@Example.TEST"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.notificationStatus").value("pending"));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(WAIT_MS)).send(eq(EMAIL), anyString(), body.capture());
        assertTrue(body.getValue().contains("Ada Holder"));
        assertTrue(body.getValue().contains("controlled"));
        assertTrue(body.getValue().contains("Controlled Zone"));
        assertTrue(body.getValue().contains(LocalDate.now().plusDays(10).toString()));

        String id = credentials.findAll().get(0).getId();
        awaitStatus(id, CredentialNotificationStatus.SENT);
    }

    @Test
    void email_shouldEscapeTheName_andKeepPersonalDataOutOfTheSubject() throws Exception {
        create(json("<img src=x onerror=alert(1)>", EMAIL)).andExpect(status().isCreated());

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(notifications, timeout(WAIT_MS)).send(anyString(), subject.capture(), body.capture());

        assertFalse(body.getValue().contains("<img"));
        assertTrue(body.getValue().contains("&lt;img src=x"));
        assertFalse(subject.getValue().contains("img"));
        assertFalse(subject.getValue().contains(EMAIL));
    }

    @Test
    void create_shouldSucceedAndReportFailure_whenTheMailIsNotDelivered() throws Exception {
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(false);

        CredentialUserEntity credential = created("Failed Delivery");

        awaitStatus(credential.getId(), CredentialNotificationStatus.FAILED);
        assertEquals("Active", credentials.findById(credential.getId()).orElseThrow().getStatus());
    }

    @Test
    void create_shouldSucceedAndReportFailure_whenTheMailLayerThrows() throws Exception {
        when(notifications.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("down"));

        CredentialUserEntity credential = created("Broken Mail");

        awaitStatus(credential.getId(), CredentialNotificationStatus.FAILED);
    }

    @Test
    void refusedCreation_shouldSendNothing() throws Exception {
        CredentialUserEntity credential = created("Only Once");
        awaitStatus(credential.getId(), CredentialNotificationStatus.SENT);
        clearInvocations(notifications);

        create(json("Only Once", EMAIL)).andExpect(status().isConflict());

        verify(notifications, noCallSoon()).send(anyString(), anyString(), anyString());
    }

    private static VerificationMode noCallSoon() {
        return after(300).never();
    }

    @Test
    void revoke_shouldEmailTheHolderOnce_evenWhenRepeated() throws Exception {
        CredentialUserEntity credential = created("Revoked Holder");
        awaitStatus(credential.getId(), CredentialNotificationStatus.SENT);

        revoke(credential.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Revoked"))
                .andExpect(jsonPath("$.notificationStatus").value("pending"));
        awaitStatus(credential.getId(), CredentialNotificationStatus.SENT);
        revoke(credential.getId()).andExpect(status().isOk());

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(notifications, after(300).times(2)).send(eq(EMAIL), subject.capture(), anyString());
        assertTrue(subject.getAllValues().get(1).contains("révoqué"));
    }

    @Test
    void revoke_shouldSucceedAndReportFailure_whenTheMailIsNotDelivered() throws Exception {
        CredentialUserEntity credential = created("Revoke Failure");
        awaitStatus(credential.getId(), CredentialNotificationStatus.SENT);
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(false);

        revoke(credential.getId()).andExpect(status().isOk());

        awaitStatus(credential.getId(), CredentialNotificationStatus.FAILED);
        assertEquals("Revoked", credentials.findById(credential.getId()).orElseThrow().getStatus());
    }

    @Test
    void revoke_shouldSendNothing_forAnExistingCredentialWithoutEmail() throws Exception {
        credentials.save(CredentialUserEntity.builder().id("legacy").name("Legacy Holder").role("Tester")
                .type("Staff").level("open").validUntil("Permanent").status("Active")
                .initials("LH").lastActive("-").build());

        revoke("legacy")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Revoked"))
                .andExpect(jsonPath("$.notificationStatus", nullValue()));

        verify(notifications, noCallSoon()).send(any(), any(), any());
        assertNull(credentials.findById("legacy").orElseThrow().getEmail());
    }

    @Test
    void email_shouldBeVisibleToAdminsOnly() throws Exception {
        CredentialUserEntity credential = created("Private Holder");
        awaitStatus(credential.getId(), CredentialNotificationStatus.SENT);

        mvc.perform(get(CREDENTIALS).header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMAIL));

        String asReader = mvc.perform(get(CREDENTIALS).header(HttpHeaders.AUTHORIZATION, "Bearer " + READ_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email", nullValue()))
                .andReturn().getResponse().getContentAsString();
        assertFalse(asReader.contains(EMAIL));
    }

    @Test
    void email_shouldBeRequiredAndValid() throws Exception {
        create(json("No Email", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        create(json("Bad Email", "not-an-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
        create(json("Long Email", "a".repeat(250) + "@example.test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());

        assertEquals(0, credentials.count());
    }

    @Test
    void moduleLogs_shouldNeverContainTheEmail(CapturedOutput output) throws Exception {
        when(notifications.send(anyString(), anyString(), anyString())).thenReturn(false);

        CredentialUserEntity credential = created("Logged Holder");
        awaitStatus(credential.getId(), CredentialNotificationStatus.FAILED);
        revoke(credential.getId()).andExpect(status().isOk());
        verify(notifications, timeout(WAIT_MS).times(2)).send(anyString(), anyString(), anyString());
        awaitStatus(credential.getId(), CredentialNotificationStatus.FAILED);

        assertTrue(output.getAll().contains(credential.getId()));
        assertFalse(output.getAll().contains(EMAIL));
        assertFalse(credential.toString().contains(EMAIL));
    }
}

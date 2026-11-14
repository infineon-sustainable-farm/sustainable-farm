package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.jayway.jsonpath.JsonPath;
import com.infineonbit.sustainablefarm.core.notification.NotificationService;
import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityPrincipal;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditAction;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.SiteSecurityAuditEventEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.SiteSecurityAuditEventRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.service.SiteSecurityAuditService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions.signedIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every administrative change keeps who made it, what it was, on which record, and when. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-audit;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SiteSecurityAuditTrailIntegrationTest {

    private static final String BASE = "/api/v1/sitesecurity";
    private static final String ADMIN_A = "admin.a@example.test";
    private static final String ADMIN_B = "admin.b@example.test";
    private static final String READER = "reader@example.test";
    private static final String TARGET = "target.person@example.test";
    private static final String HOLDER_NAME = "Holder Person";
    private static final String HOLDER_EMAIL = "holder.person@example.test";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository operators;

    @Autowired
    private OperatorSessionRepository sessions;

    @Autowired
    private CredentialUserRepository credentials;

    @Autowired
    private SiteSecurityAuditEventRepository audit;

    @MockitoBean
    private NotificationService notifications;

    @MockitoSpyBean
    private SiteSecurityAuditService auditService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String sessionA;
    private String sessionB;
    private String readerSession;
    private String adminA;
    private String adminB;

    @BeforeEach
    void resetData() {
        audit.deleteAll();
        sessions.deleteAll();
        credentials.deleteAll();
        operators.deleteAll();
        sessionA = TestSessions.open(operators, sessions, ADMIN_A, SiteSecurityRole.ADMIN);
        sessionB = TestSessions.open(operators, sessions, ADMIN_B, SiteSecurityRole.ADMIN);
        readerSession = TestSessions.open(operators, sessions, READER, SiteSecurityRole.READ);
        adminA = idOf(ADMIN_A);
        adminB = idOf(ADMIN_B);
    }

    private String idOf(String email) {
        return operators.findAll().stream().filter(o -> o.getEmail().equals(email)).findFirst().orElseThrow().getId();
    }

    private String target(SiteSecurityRole role) {
        return operators.save(OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(TARGET)
                .fullName("Target Person")
                .category("WORKER")
                .role(role)
                .active(true)
                .createdBy("test")
                .createdAt(Instant.now())
                .build()).getId();
    }

    private ResultActions changeRole(String session, String id, String role) throws Exception {
        String apiRole = "read".equals(role) ? "user" : role;
        return mvc.perform(patch(BASE + "/users/" + id).with(signedIn(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Target Person\",\"category\":\"worker\",\"role\":\"" + apiRole + "\"}"));
    }

    private ResultActions operatorAction(String session, String id, String action) throws Exception {
        return mvc.perform(post(BASE + "/users/" + id + "/" + action).with(signedIn(session)));
    }

    private ResultActions createCredential(String session) throws Exception {
        return mvc.perform(post(BASE + "/credentials").with(signedIn(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"" + HOLDER_NAME + "\",\"email\":\"" + HOLDER_EMAIL
                        + "\",\"role\":\"Tester\",\"type\":\"Staff\",\"level\":\"open\",\"validUntil\":\""
                        + LocalDate.now().plusMonths(1) + "\"}"));
    }

    private List<SiteSecurityAuditEventEntity> eventsFor(String targetId) {
        return audit.findByTargetIdOrderByOccurredAtAsc(targetId);
    }

    private static void assertEvent(SiteSecurityAuditEventEntity event, SiteSecurityAuditAction action,
            String actorId, String targetType, String targetId, Instant notBefore) {
        assertEquals(action, event.getAction());
        assertEquals(actorId, event.getActorOperatorId());
        assertEquals(targetType, event.getTargetType());
        assertEquals(targetId, event.getTargetId());
        assertFalse(event.getOccurredAt().isBefore(notBefore));
        assertFalse(event.getOccurredAt().isAfter(Instant.now()));
    }

    @Test
    void roleChange_shouldRecordWhoChangedIt_fromWhatToWhat() throws Exception {
        String id = target(SiteSecurityRole.READ);
        Instant start = Instant.now();

        changeRole(sessionA, id, "admin").andExpect(status().isOk());
        changeRole(sessionB, id, "read").andExpect(status().isOk());

        List<SiteSecurityAuditEventEntity> events = eventsFor(id);
        assertEquals(2, events.size());
        assertEvent(events.get(0), SiteSecurityAuditAction.OPERATOR_ROLE_CHANGED, adminA, "OPERATOR", id, start);
        assertEquals("user->admin", events.get(0).getDetail());
        assertEvent(events.get(1), SiteSecurityAuditAction.OPERATOR_ROLE_CHANGED, adminB, "OPERATOR", id, start);
        assertEquals("admin->user", events.get(1).getDetail());
    }

    @Test
    void settingTheSameRole_shouldRecordNothing() throws Exception {
        String id = target(SiteSecurityRole.READ);

        changeRole(sessionA, id, "read").andExpect(status().isOk());

        assertTrue(eventsFor(id).isEmpty());
    }

    @Test
    void reEnabling_shouldKeepWhoDisabledTheOperator_inTheTrail() throws Exception {
        String id = target(SiteSecurityRole.READ);
        Instant start = Instant.now();

        operatorAction(sessionA, id, "disable").andExpect(status().isOk())
                .andExpect(jsonPath("$.disabledBy").value(ADMIN_A));
        operatorAction(sessionB, id, "enable").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));

        // The row shows the current state; the trail keeps the history.
        assertNull(operators.findById(id).orElseThrow().getDisabledBy());
        List<SiteSecurityAuditEventEntity> events = eventsFor(id);
        assertEquals(2, events.size());
        assertEvent(events.get(0), SiteSecurityAuditAction.OPERATOR_DISABLED, adminA, "OPERATOR", id, start);
        assertEvent(events.get(1), SiteSecurityAuditAction.OPERATOR_ENABLED, adminB, "OPERATOR", id, start);
    }

    @Test
    void disablingAgainAfterReEnabling_shouldAddEvents_neverReplaceThem() throws Exception {
        String id = target(SiteSecurityRole.READ);

        operatorAction(sessionA, id, "disable").andExpect(status().isOk());
        operatorAction(sessionB, id, "enable").andExpect(status().isOk());
        operatorAction(sessionB, id, "disable").andExpect(status().isOk());

        List<SiteSecurityAuditEventEntity> events = eventsFor(id);
        assertEquals(List.of(SiteSecurityAuditAction.OPERATOR_DISABLED, SiteSecurityAuditAction.OPERATOR_ENABLED,
                SiteSecurityAuditAction.OPERATOR_DISABLED), events.stream().map(SiteSecurityAuditEventEntity::getAction).toList());
        assertEquals(List.of(adminA, adminB, adminB),
                events.stream().map(SiteSecurityAuditEventEntity::getActorOperatorId).toList());
    }

    @Test
    void repeatedDisableOrEnable_shouldRecordEachChangeOnce() throws Exception {
        String id = target(SiteSecurityRole.READ);

        operatorAction(sessionA, id, "disable").andExpect(status().isOk());
        operatorAction(sessionA, id, "disable").andExpect(status().isOk());
        operatorAction(sessionA, id, "enable").andExpect(status().isOk());
        operatorAction(sessionA, id, "enable").andExpect(status().isOk());

        assertEquals(2, eventsFor(id).size());
    }

    @Test
    void refusedChange_shouldRecordNothing() throws Exception {
        operatorAction(sessionB, adminB, "disable").andExpect(status().isOk());

        // Admin A is now the last active admin: demoting or disabling them is refused.
        changeRole(sessionA, adminA, "read").andExpect(status().is4xxClientError());
        operatorAction(sessionA, adminA, "disable").andExpect(status().is4xxClientError());

        assertTrue(eventsFor(adminA).isEmpty());
    }

    @Test
    void readRole_shouldNotBeAbleToMakeChanges_norLeaveEvents() throws Exception {
        String id = target(SiteSecurityRole.READ);

        operatorAction(readerSession, id, "disable").andExpect(status().isForbidden());
        createCredential(readerSession).andExpect(status().isForbidden());

        assertEquals(0, audit.count());
    }

    @Test
    void addingAnOperator_shouldRecordWhoAddedThem() throws Exception {
        Instant start = Instant.now();
        String body = mvc.perform(post(BASE + "/users").with(signedIn(sessionA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"New One\",\"email\":\"new.one@example.test\",\"emailConfirmation\":\"new.one@example.test\",\"role\":\"user\",\"category\":\"worker\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        // Only the invitation itself: the email outcome is recorded separately, by the system.
        List<SiteSecurityAuditEventEntity> events = eventsFor(id).stream()
                .filter(e -> e.getAction() == SiteSecurityAuditAction.ACCOUNT_INVITED).toList();
        assertEquals(1, events.size());
        assertEvent(events.get(0), SiteSecurityAuditAction.ACCOUNT_INVITED, adminA, "OPERATOR", id, start);
        assertEquals("user worker", events.get(0).getDetail());
    }

    @Test
    void credentialCreationAndRevocation_shouldBeAttributedToTheirAdmins() throws Exception {
        Instant start = Instant.now();
        String body = createCredential(sessionA).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mvc.perform(post(BASE + "/credentials/" + id + "/revoke").with(signedIn(sessionB)))
                .andExpect(status().isOk());
        mvc.perform(post(BASE + "/credentials/" + id + "/revoke").with(signedIn(sessionA)))
                .andExpect(status().isOk());

        List<SiteSecurityAuditEventEntity> events = eventsFor(id);
        assertEquals(2, events.size());
        assertEvent(events.get(0), SiteSecurityAuditAction.CREDENTIAL_CREATED, adminA, "CREDENTIAL", id, start);
        assertEquals("open", events.get(0).getDetail());
        assertEvent(events.get(1), SiteSecurityAuditAction.CREDENTIAL_REVOKED, adminB, "CREDENTIAL", id, start);
    }

    @Test
    void failedCredentialCreation_shouldRecordNothing() throws Exception {
        createCredential(sessionA).andExpect(status().isCreated());
        long before = audit.count();

        createCredential(sessionA).andExpect(status().isConflict());

        assertEquals(before, audit.count());
    }

    @Test
    void auditFailure_shouldUndoTheChange() throws Exception {
        String id = target(SiteSecurityRole.READ);
        String targetSession = TestSessions.openFor(sessions, id, Duration.ofHours(8));
        // Stub the spy behind the transactional proxy: calling the proxy here would need a transaction.
        SiteSecurityAuditService spy = AopTestUtils.getUltimateTargetObject(auditService);
        doThrow(new IllegalStateException("audit storage unavailable"))
                .when(spy).record(any(), eq(SiteSecurityAuditAction.OPERATOR_DISABLED), any(), any());

        operatorAction(sessionA, id, "disable").andExpect(status().is5xxServerError());

        OperatorEntity unchanged = operators.findById(id).orElseThrow();
        assertTrue(unchanged.isActive());
        assertNull(unchanged.getDisabledBy());
        // The session revocation is rolled back too: the operator is still signed in.
        mvc.perform(get(BASE + "/session").with(signedIn(targetSession))).andExpect(status().isOk());
        assertTrue(eventsFor(id).isEmpty());
    }

    @Test
    void audit_shouldRefuseToRunOutsideTheChangesTransaction() {
        SiteSecurityPrincipal admin = new SiteSecurityPrincipal(adminA, ADMIN_A, SiteSecurityRole.ADMIN);

        assertThrows(IllegalTransactionStateException.class, () -> auditService.record(
                admin, SiteSecurityAuditAction.OPERATOR_ENABLED, adminB, null));
        assertEquals(0, audit.count());
    }

    @Test
    void audit_shouldRefuseAnAnonymousOrGateScannerActor() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status ->
                auditService.record(SiteSecurityPrincipal.gateScanner(), SiteSecurityAuditAction.OPERATOR_ENABLED, adminB, null)));
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status ->
                auditService.record(null, SiteSecurityAuditAction.OPERATOR_ENABLED, adminB, null)));
        assertEquals(0, audit.count());
    }

    @Test
    void trailAndLogs_shouldHoldIdsOnly_neverEmailsOrNames(CapturedOutput output) throws Exception {
        String id = target(SiteSecurityRole.READ);
        changeRole(sessionA, id, "admin").andExpect(status().isOk());
        operatorAction(sessionA, id, "disable").andExpect(status().isOk());
        operatorAction(sessionA, id, "enable").andExpect(status().isOk());
        createCredential(sessionA).andExpect(status().isCreated());

        for (SiteSecurityAuditEventEntity event : audit.findAll()) {
            String row = event.toString();
            assertFalse(row.contains("@"), row);
            assertFalse(row.contains(HOLDER_NAME), row);
        }
        String moduleLines = output.getAll().lines()
                .filter(line -> line.contains("Site Security audit") && line.contains(" by admin "))
                .reduce("", (a, b) -> a + b + "\n");
        assertEquals(4, moduleLines.lines().count(), moduleLines);
        assertTrue(moduleLines.contains(adminA));
        assertFalse(moduleLines.contains(TARGET));
        assertFalse(moduleLines.contains(ADMIN_A));
        assertFalse(moduleLines.contains(HOLDER_EMAIL));
        assertFalse(moduleLines.contains(HOLDER_NAME));
    }
}

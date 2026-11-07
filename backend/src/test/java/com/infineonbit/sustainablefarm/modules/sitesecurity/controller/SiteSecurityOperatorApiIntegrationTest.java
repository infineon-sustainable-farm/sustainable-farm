package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions.signedIn;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-operators;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityOperatorApiIntegrationTest {

    /** The signed-in admin is an operator too, so it counts as an active admin. */
    private static final String CALLER = "session-admin@example.test";
    private static final String READER = "session-reader@example.test";
    private static final String OPERATORS = "/api/v1/sitesecurity/operators";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository operators;

    @Autowired
    private OperatorSessionRepository sessions;

    private String readSession;
    private String adminSession;

    @BeforeEach
    void resetData() {
        sessions.deleteAll();
        operators.deleteAll();
        readSession = TestSessions.open(operators, sessions, READER, SiteSecurityRole.READ);
        adminSession = TestSessions.open(operators, sessions, CALLER, SiteSecurityRole.ADMIN);
    }

    private OperatorEntity caller() {
        return operators.findAll().stream().filter(o -> o.getEmail().equals(CALLER)).findFirst().orElseThrow();
    }

    private OperatorEntity reader() {
        return operators.findAll().stream().filter(o -> o.getEmail().equals(READER)).findFirst().orElseThrow();
    }

    private OperatorEntity existing(String email, SiteSecurityRole role, boolean active) {
        return operators.save(OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(email)
                .role(role)
                .active(active)
                .createdBy("test")
                .createdAt(Instant.now())
                .build());
    }

    private ResultActions add(String body) throws Exception {
        return mvc.perform(post(OPERATORS)
                .with(signedIn(adminSession))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions action(String id, String name) throws Exception {
        return mvc.perform(post(OPERATORS + "/" + id + "/" + name)
                .with(signedIn(adminSession)));
    }

    private ResultActions changeRole(String id, String role) throws Exception {
        return mvc.perform(put(OPERATORS + "/" + id + "/role")
                .with(signedIn(adminSession))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"" + role + "\"}"));
    }

    @Test
    void operators_shouldRefuseAnonymousCallers() throws Exception {
        mvc.perform(get(OPERATORS)).andExpect(status().isUnauthorized());
    }

    @Test
    void operators_shouldBeHiddenFromTheReadRole_evenForReads() throws Exception {
        mvc.perform(get(OPERATORS).with(signedIn(readSession)))
                .andExpect(status().isForbidden());
    }

    @Test
    void operators_shouldStayHiddenFromTheReadRole_whenThePathIsEncoded() throws Exception {
        mvc.perform(get(URI.create("/api/v1/sitesecurity/%6Fperators"))
                        .with(signedIn(readSession)))
                .andExpect(status().isForbidden());
    }


    @Test
    void add_shouldRefuseTheReadRole() throws Exception {
        mvc.perform(post(OPERATORS)
                        .with(signedIn(readSession))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.test\",\"role\":\"read\"}"))
                .andExpect(status().isForbidden());

        assertEquals(2, operators.count());
    }

    @Test
    void add_shouldStoreANormalizedActiveOperator_andRecordWhoAddedIt() throws Exception {
        add("{\"email\":\"New.Operator@Example.TEST\",\"role\":\"read\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.operator@example.test"))
                .andExpect(jsonPath("$.role").value("read"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").value(CALLER))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void add_shouldRefuseADuplicate_whateverTheCase() throws Exception {
        existing("taken@example.test", SiteSecurityRole.READ, true);

        add("{\"email\":\"TAKEN@example.test\",\"role\":\"admin\"}").andExpect(status().isConflict());

        assertEquals(3, operators.count());
    }

    @Test
    void add_shouldRejectAnInvalidEmailOrRole() throws Exception {
        add("{\"email\":\"not-an-email\",\"role\":\"read\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email", notNullValue()));
        add("{\"email\":\"ok@example.test\",\"role\":\"gate\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role", notNullValue()));
    }

    @Test
    void list_shouldBeSortedByEmail() throws Exception {
        existing("zoe@example.test", SiteSecurityRole.READ, true);
        existing("amal@example.test", SiteSecurityRole.ADMIN, true);

        mvc.perform(get(OPERATORS).with(signedIn(adminSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].email", contains("amal@example.test", CALLER, READER, "zoe@example.test")));
    }

    @Test
    void disable_shouldRecordWhoAndWhen_thenEnableShouldClearIt() throws Exception {
        OperatorEntity operator = existing("reader@example.test", SiteSecurityRole.READ, true);

        action(operator.getId(), "disable")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.disabledBy").value(CALLER))
                .andExpect(jsonPath("$.disabledAt", notNullValue()));

        action(operator.getId(), "enable")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.disabledBy", nullValue()))
                .andExpect(jsonPath("$.disabledAt", nullValue()));
    }

    @Test
    void disable_shouldKeepTheFirstRecord_whenCalledTwice() throws Exception {
        OperatorEntity operator = existing("reader@example.test", SiteSecurityRole.READ, true);

        action(operator.getId(), "disable").andExpect(status().isOk());
        Instant firstDisabledAt = operators.findById(operator.getId()).orElseThrow().getDisabledAt();

        action(operator.getId(), "disable").andExpect(status().isOk());

        assertEquals(firstDisabledAt, operators.findById(operator.getId()).orElseThrow().getDisabledAt());
    }

    @Test
    void disable_shouldEndTheOperatorsSessionsAtOnce() throws Exception {
        mvc.perform(get("/api/v1/sitesecurity/session").with(signedIn(readSession))).andExpect(status().isOk());

        action(reader().getId(), "disable").andExpect(status().isOk());

        mvc.perform(get("/api/v1/sitesecurity/session").with(signedIn(readSession)))
                .andExpect(status().isUnauthorized());
        assertEquals(0, sessions.countByOperatorIdAndRevokedAtIsNullAndExpiresAtAfter(reader().getId(), Instant.now()));

        // Enabling the operator again does not reopen the old session: they sign in again.
        action(reader().getId(), "enable").andExpect(status().isOk());
        mvc.perform(get("/api/v1/sitesecurity/session").with(signedIn(readSession)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChange_shouldApplyToTheNextRequest_withoutSigningInAgain() throws Exception {
        mvc.perform(get(OPERATORS).with(signedIn(readSession))).andExpect(status().isForbidden());

        changeRole(reader().getId(), "admin").andExpect(status().isOk());
        mvc.perform(get(OPERATORS).with(signedIn(readSession))).andExpect(status().isOk());

        changeRole(reader().getId(), "read").andExpect(status().isOk());
        mvc.perform(get(OPERATORS).with(signedIn(readSession))).andExpect(status().isForbidden());
    }

    @Test
    void lastActiveAdmin_shouldNotDisableThemselves() throws Exception {
        action(caller().getId(), "disable").andExpect(status().isUnprocessableContent());

        assertTrue(caller().isActive());
    }

    @Test
    void lastActiveAdmin_shouldNotDemoteThemselves() throws Exception {
        changeRole(caller().getId(), "read").andExpect(status().isUnprocessableContent());

        assertEquals(SiteSecurityRole.ADMIN, caller().getRole());
    }

    @Test
    void aDisabledAdmin_shouldNotCountAsTheRemainingAdmin() throws Exception {
        existing("former@example.test", SiteSecurityRole.ADMIN, false);

        action(caller().getId(), "disable").andExpect(status().isUnprocessableContent());
    }

    @Test
    void anAdmin_shouldBeDisabledOrDemoted_whenAnotherActiveAdminRemains() throws Exception {
        OperatorEntity first = existing("first@example.test", SiteSecurityRole.ADMIN, true);
        OperatorEntity second = existing("second@example.test", SiteSecurityRole.ADMIN, true);

        action(first.getId(), "disable").andExpect(status().isOk());
        changeRole(second.getId(), "read")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("read"));

        changeRole(caller().getId(), "read").andExpect(status().isUnprocessableContent());
    }

    @Test
    void anAdmin_shouldDisableThemselves_whenAnotherActiveAdminRemains() throws Exception {
        existing("other-admin@example.test", SiteSecurityRole.ADMIN, true);

        action(caller().getId(), "disable").andExpect(status().isOk());

        mvc.perform(get(OPERATORS).with(signedIn(adminSession))).andExpect(status().isUnauthorized());
    }

    @Test
    void aDisabledAdmin_shouldBeDemoted_evenWhenOneActiveAdminRemains() throws Exception {
        OperatorEntity former = existing("former@example.test", SiteSecurityRole.ADMIN, false);

        changeRole(former.getId(), "read").andExpect(status().isOk());
    }

    @Test
    void changeRole_shouldPromoteAReader() throws Exception {
        OperatorEntity operator = existing("reader@example.test", SiteSecurityRole.READ, true);

        changeRole(operator.getId(), "admin")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    void changeRole_shouldRejectAnUnknownRole() throws Exception {
        OperatorEntity operator = existing("reader@example.test", SiteSecurityRole.READ, true);

        changeRole(operator.getId(), "root").andExpect(status().isBadRequest());
        changeRole(operator.getId(), "gate").andExpect(status().isBadRequest());
    }

    @Test
    void unknownOperator_shouldReturn404() throws Exception {
        action("missing", "disable").andExpect(status().isNotFound());
        action("missing", "enable").andExpect(status().isNotFound());
        changeRole("missing", "read").andExpect(status().isNotFound());
    }
}

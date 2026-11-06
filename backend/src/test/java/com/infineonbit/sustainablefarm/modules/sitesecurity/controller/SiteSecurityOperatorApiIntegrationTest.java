package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
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
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-operators;DB_CLOSE_DELAY=-1",
        "SITESECURITY_READ_TOKEN=" + SiteSecurityOperatorApiIntegrationTest.READ_TOKEN,
        "SITESECURITY_ADMIN_TOKEN=" + SiteSecurityOperatorApiIntegrationTest.ADMIN_TOKEN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityOperatorApiIntegrationTest {

    static final String READ_TOKEN = "read-token-for-operator-tests-0123456789";
    static final String ADMIN_TOKEN = "admin-token-for-operator-tests-0123456789";

    private static final String OPERATORS = "/api/v1/sitesecurity/operators";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository operators;

    @BeforeEach
    void resetData() {
        operators.deleteAll();
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
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions action(String id, String name) throws Exception {
        return mvc.perform(post(OPERATORS + "/" + id + "/" + name)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN));
    }

    private ResultActions changeRole(String id, String role) throws Exception {
        return mvc.perform(put(OPERATORS + "/" + id + "/role")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"" + role + "\"}"));
    }

    @Test
    void operators_shouldRefuseAnonymousCallers() throws Exception {
        mvc.perform(get(OPERATORS)).andExpect(status().isUnauthorized());
    }

    @Test
    void operators_shouldBeHiddenFromTheReadRole_evenForReads() throws Exception {
        mvc.perform(get(OPERATORS).header(HttpHeaders.AUTHORIZATION, "Bearer " + READ_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void operators_shouldStayHiddenFromTheReadRole_whenThePathIsEncoded() throws Exception {
        mvc.perform(get(URI.create("/api/v1/sitesecurity/%6Fperators"))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + READ_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void add_shouldRefuseTheReadRole() throws Exception {
        mvc.perform(post(OPERATORS)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + READ_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@example.test\",\"role\":\"read\"}"))
                .andExpect(status().isForbidden());

        assertEquals(0, operators.count());
    }

    @Test
    void add_shouldStoreANormalizedActiveOperator() throws Exception {
        add("{\"email\":\"New.Operator@Example.TEST\",\"role\":\"read\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.operator@example.test"))
                .andExpect(jsonPath("$.role").value("read"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdBy").value("admin-token"))
                .andExpect(jsonPath("$.createdAt", notNullValue()));
    }

    @Test
    void add_shouldRefuseADuplicate_whateverTheCase() throws Exception {
        existing("taken@example.test", SiteSecurityRole.READ, true);

        add("{\"email\":\"TAKEN@example.test\",\"role\":\"admin\"}").andExpect(status().isConflict());

        assertEquals(1, operators.count());
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

        mvc.perform(get(OPERATORS).header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].email", contains("amal@example.test", "zoe@example.test")));
    }

    @Test
    void disable_shouldRecordWhoAndWhen_thenEnableShouldClearIt() throws Exception {
        existing("admin@example.test", SiteSecurityRole.ADMIN, true);
        OperatorEntity operator = existing("reader@example.test", SiteSecurityRole.READ, true);

        action(operator.getId(), "disable")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.disabledBy").value("admin-token"))
                .andExpect(jsonPath("$.disabledAt", notNullValue()));

        action(operator.getId(), "enable")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.disabledBy", nullValue()))
                .andExpect(jsonPath("$.disabledAt", nullValue()));
    }

    @Test
    void disable_shouldKeepTheFirstRecord_whenCalledTwice() throws Exception {
        existing("admin@example.test", SiteSecurityRole.ADMIN, true);
        OperatorEntity operator = existing("reader@example.test", SiteSecurityRole.READ, true);

        action(operator.getId(), "disable").andExpect(status().isOk());
        Instant firstDisabledAt = operators.findById(operator.getId()).orElseThrow().getDisabledAt();

        action(operator.getId(), "disable").andExpect(status().isOk());

        assertEquals(firstDisabledAt, operators.findById(operator.getId()).orElseThrow().getDisabledAt());
    }

    @Test
    void lastActiveAdmin_shouldNotBeDisabled() throws Exception {
        OperatorEntity admin = existing("admin@example.test", SiteSecurityRole.ADMIN, true);

        action(admin.getId(), "disable").andExpect(status().isUnprocessableContent());

        assertTrue(operators.findById(admin.getId()).orElseThrow().isActive());
    }

    @Test
    void lastActiveAdmin_shouldNotBeDemoted() throws Exception {
        OperatorEntity admin = existing("admin@example.test", SiteSecurityRole.ADMIN, true);

        changeRole(admin.getId(), "read").andExpect(status().isUnprocessableContent());

        assertEquals(SiteSecurityRole.ADMIN, operators.findById(admin.getId()).orElseThrow().getRole());
    }

    @Test
    void aDisabledAdmin_shouldNotCountAsTheRemainingAdmin() throws Exception {
        OperatorEntity admin = existing("admin@example.test", SiteSecurityRole.ADMIN, true);
        existing("former@example.test", SiteSecurityRole.ADMIN, false);

        action(admin.getId(), "disable").andExpect(status().isUnprocessableContent());
    }

    @Test
    void anAdmin_shouldBeDisabledOrDemoted_whenAnotherActiveAdminRemains() throws Exception {
        OperatorEntity first = existing("first@example.test", SiteSecurityRole.ADMIN, true);
        OperatorEntity second = existing("second@example.test", SiteSecurityRole.ADMIN, true);
        OperatorEntity third = existing("third@example.test", SiteSecurityRole.ADMIN, true);

        action(first.getId(), "disable").andExpect(status().isOk());
        changeRole(second.getId(), "read")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("read"));

        changeRole(third.getId(), "read").andExpect(status().isUnprocessableContent());
    }

    @Test
    void aDisabledAdmin_shouldBeDemoted_evenWhenOneActiveAdminRemains() throws Exception {
        existing("admin@example.test", SiteSecurityRole.ADMIN, true);
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
    }

    @Test
    void unknownOperator_shouldReturn404() throws Exception {
        action("missing", "disable").andExpect(status().isNotFound());
        action("missing", "enable").andExpect(status().isNotFound());
        changeRole("missing", "read").andExpect(status().isNotFound());
    }
}

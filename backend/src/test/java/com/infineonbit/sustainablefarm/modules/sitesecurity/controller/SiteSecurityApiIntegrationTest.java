package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs without the dev profile, as in production: only the reference zones exist at startup. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-api;DB_CLOSE_DELAY=-1",
        "SITESECURITY_READ_TOKEN=" + SiteSecurityApiIntegrationTest.READ_TOKEN,
        "SITESECURITY_ADMIN_TOKEN=" + SiteSecurityApiIntegrationTest.ADMIN_TOKEN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityApiIntegrationTest {

    static final String READ_TOKEN = "read-token-for-api-tests-0123456789abcdef";
    static final String ADMIN_TOKEN = "admin-token-for-api-tests-0123456789abcdef";

    private static final String BASE = "/api/v1/sitesecurity";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CredentialUserRepository credentials;

    @Autowired
    private GateRepository gates;

    @Autowired
    private LogEntryRepository logs;

    @BeforeEach
    void resetData() {
        credentials.deleteAll();
        gates.deleteAll();
        logs.deleteAll();
    }

    private ResultActions read(String path) throws Exception {
        return mvc.perform(get(BASE + path).header(HttpHeaders.AUTHORIZATION, "Bearer " + READ_TOKEN));
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post(BASE + "/credentials")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions revoke(String token, String id) throws Exception {
        return mvc.perform(post(BASE + "/credentials/" + id + "/revoke")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private String json(String type, String level, String validUntil) {
        return "{\"name\":\"Some One\",\"role\":\"Tester\",\"type\":\"" + type
                + "\",\"level\":\"" + level + "\",\"validUntil\":\"" + validUntil + "\"}";
    }

    private String inOneMonth() {
        return LocalDate.now().plusMonths(1).toString();
    }

    private CredentialUserEntity credential(String id, String type, String level, String validUntil, String status) {
        return credentials.save(CredentialUserEntity.builder().id(id).name("Name " + id).role("Tester")
                .type(type).level(level).validUntil(validUntil).status(status).initials("NN").lastActive("-").build());
    }

    private void gate(String id, String zone, String status) {
        gates.save(GateEntity.builder().id(id).name(id).shortName(id).zone(zone).status(status).detail("-").build());
    }

    private void logEntry(String id, String date, String time, String status, String type) {
        logs.save(LogEntryEntity.builder().id(id).ref(id).date(date).time(time).status(status).type(type)
                .user("Someone").initials("SO").zone("open").zoneName("Open Zone").action("Entry")
                .gate("Gate").method("Card").note("-").build());
    }

    // --- reference data ---

    @Test
    void zones_shouldExistWithoutTheDevProfile() throws Exception {
        read("/zones")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)));
        read("/credentials").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void credential_shouldBeCreatable_withoutTheDevProfile() throws Exception {
        create(json("Staff", "controlled", inOneMonth()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lastActive").value("Not yet on site"));
    }

    // --- HTTP errors ---

    @Test
    void invalidFields_shouldReturn400WithFieldErrors() throws Exception {
        create(json("Superuser", "open", "Permanent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").exists());
        create(json("Staff", "open", "31 Dec 2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.validUntil").exists());
    }

    @Test
    void businessRuleViolations_shouldReturn422WithAReadableMessage() throws Exception {
        create(json("Staff", "unknown-zone", inOneMonth()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value(
                        "Unknown access level 'unknown-zone': no zone with this id is defined."));
        create(json("Staff", "open", "2020-01-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.message").value("validUntil '2020-01-01' is in the past."));
        create(json("Staff", "open", "2027-02-31"))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void permanent_shouldBeStoredInItsCanonicalForm() throws Exception {
        create(json("Staff", "open", "permanent"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.validUntil").value("Permanent"));
    }

    // --- log filtering ---

    @Test
    void unknownLogFilter_shouldReturn400() throws Exception {
        read("/logs?filter=garbage").andExpect(status().isBadRequest());
    }

    @Test
    void logs_shouldBeSortedNewestFirstForEveryFilter() throws Exception {
        logEntry("old", "2026-09-05", "08:00:00", "Denied", "Staff");
        logEntry("new", "2026-09-06", "07:00:00", "Denied", "Staff");
        logEntry("mid", "2026-09-05", "09:00:00", "Approved", "Visitor");

        read("/logs").andExpect(jsonPath("$[*].id", contains("new", "mid", "old")));
        read("/logs?filter=all").andExpect(jsonPath("$[*].id", contains("new", "mid", "old")));
        read("/logs?filter=denied").andExpect(jsonPath("$[*].id", contains("new", "old")));
        read("/logs?filter=DENIED").andExpect(jsonPath("$[*].id", contains("new", "old")));
        read("/logs?filter=staff").andExpect(jsonPath("$[*].id", contains("new", "old")));
        read("/logs?filter=visitors").andExpect(jsonPath("$[*].id", contains("mid")));
    }

    // --- computed values ---

    @Test
    void overview_shouldReflectTheDatabase() throws Exception {
        gate("g1", "open", "Active");
        gate("g2", "restricted", "Secure");
        gate("g3", "critical", "Locked");
        credential("v1", "Visitor", "open", inOneMonth(), "Active");
        credential("v2", "Visitor", "open", "2020-01-01", "Active");
        credential("v3", "Visitor", "open", inOneMonth(), "Revoked");
        credential("s1", "Staff", "controlled", "Permanent", "Active");
        logEntry("l1", "2026-09-05", "08:00:00", "Denied", "Unknown");
        logEntry("l2", "2026-09-05", "08:01:00", "Denied", "Unknown");
        logEntry("l3", "2026-09-05", "08:02:00", "Approved", "Staff");

        read("/overview")
                .andExpect(jsonPath("$.gates", hasSize(3)))
                .andExpect(jsonPath("$.stats.activePoints").value(2))
                .andExpect(jsonPath("$.stats.controlledZones").value(3))
                .andExpect(jsonPath("$.stats.totalZones").value(4))
                .andExpect(jsonPath("$.stats.visitors").value(1))
                .andExpect(jsonPath("$.stats.alerts").value(2));
    }

    @Test
    void overview_shouldBeEmptyWhenThereIsNoData() throws Exception {
        read("/overview")
                .andExpect(jsonPath("$.stats.activePoints").value(0))
                .andExpect(jsonPath("$.stats.visitors").value(0))
                .andExpect(jsonPath("$.stats.alerts").value(0));
    }

    @Test
    void zones_shouldCountValidCredentialsAtOrAboveTheirLevel_andTheirGates() throws Exception {
        gate("g1", "controlled", "Active");
        gate("g2", "controlled", "Active");
        credential("a", "Staff", "open", "Permanent", "Active");
        credential("b", "Staff", "controlled", "Permanent", "Active");
        credential("c", "Security", "critical", "Permanent", "Authorized");
        credential("d", "Staff", "critical", "2020-01-01", "Active");
        credential("e", "Staff", "critical", "Permanent", "Revoked");

        read("/zones")
                .andExpect(jsonPath("$[?(@.id=='open')].authorizedUsers", contains(3)))
                .andExpect(jsonPath("$[?(@.id=='controlled')].authorizedUsers", contains(2)))
                .andExpect(jsonPath("$[?(@.id=='critical')].authorizedUsers", contains(1)))
                .andExpect(jsonPath("$[?(@.id=='controlled')].entryPoints", contains(2)))
                .andExpect(jsonPath("$[?(@.id=='open')].entryPoints", contains(0)));
    }

    // --- credential life cycle ---

    @Test
    void credential_shouldAppearExpiredOnceItsDateHasPassed() throws Exception {
        credential("old", "Staff", "open", LocalDate.now().minusDays(1).toString(), "Active");
        credential("today", "Staff", "open", LocalDate.now().toString(), "Active");
        credential("unreadable", "Staff", "open", "Today, 18:00", "Authorized");

        read("/credentials")
                .andExpect(jsonPath("$[?(@.id=='old')].status", contains("Expired")))
                .andExpect(jsonPath("$[?(@.id=='today')].status", contains("Active")))
                .andExpect(jsonPath("$[?(@.id=='unreadable')].status", contains("Expired")));
    }

    @Test
    void revoke_shouldRequireTheAdminRole() throws Exception {
        credential("c1", "Staff", "open", "Permanent", "Active");

        revoke(READ_TOKEN, "c1").andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/credentials/c1/revoke")).andExpect(status().isUnauthorized());

        read("/credentials").andExpect(jsonPath("$[0].status").value("Active"));
    }

    @Test
    void revoke_shouldBeFinalAndIdempotent() throws Exception {
        credential("c1", "Staff", "critical", "Permanent", "Active");

        revoke(ADMIN_TOKEN, "c1").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Revoked"));
        revoke(ADMIN_TOKEN, "c1").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Revoked"));

        read("/credentials").andExpect(jsonPath("$[0].status").value("Revoked"));
        read("/zones").andExpect(jsonPath("$[?(@.id=='critical')].authorizedUsers", contains(0)));
    }

    @Test
    void revoke_shouldReturn404ForAnUnknownCredential() throws Exception {
        revoke(ADMIN_TOKEN, "missing").andExpect(status().isNotFound());
    }
}

package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import java.util.List;
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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the API as a client sees it. Open-in-view is off on purpose: every
 * read must work inside its own transaction, without relying on a session kept
 * open during serialization.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-contract;DB_CLOSE_DELAY=-1",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "dev"})
class SiteSecurityHttpContractTest {


    private static final String BASE = "/api/v1/sitesecurity";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OperatorRepository sessionOperators;

    @Autowired
    private OperatorSessionRepository sessions;

    private String readSession;
    private String adminSession;

    @BeforeEach
    void openSessions() {
        readSession = TestSessions.open(sessionOperators, sessions, "session-reader@example.test", SiteSecurityRole.READ);
        adminSession = TestSessions.open(sessionOperators, sessions, "session-admin@example.test", SiteSecurityRole.ADMIN);
    }

    private ResultActions read(String path) throws Exception {
        return mvc.perform(get(BASE + path).with(signedIn(readSession)));
    }

    private ResultActions create(String body) throws Exception {
        return mvc.perform(post(BASE + "/credentials")
                .with(signedIn(adminSession))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    // --- reads, with no session left open ---

    @Test
    void zones_shouldIncludeTheirRequirements() throws Exception {
        read("/zones")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].requirements", not(hasSize(0))));
    }

    @Test
    void everyReadEndpoint_shouldAnswer() throws Exception {
        for (String path : List.of("/session", "/overview", "/credentials", "/logs", "/logs?filter=denied")) {
            read(path).andExpect(status().isOk());
        }
    }

    // --- access control ---

    @Test
    void headRequests_shouldNotBypassTheToken() throws Exception {
        mvc.perform(head(BASE + "/logs")).andExpect(status().isUnauthorized());
    }

    @Test
    void pathVariations_shouldNotExposeData() throws Exception {
        for (String path : List.of(
                BASE + "/logs/",
                "/API/V1/SITESECURITY/LOGS",
                "/api/v1/sitesecurity/LOGS",
                "/api/v1/sitesecurity/logs;x=1",
                "/api/v1/sitesecurity/logs.json")) {
            String body = mvc.perform(get(path)).andReturn().getResponse().getContentAsString();

            assertFalse(body.contains("LOG-8821"), path);
        }
    }

    @Test
    void readToken_shouldNotUseAnyWriteMethod() throws Exception {
        mvc.perform(delete(BASE + "/credentials/u1").with(signedIn(readSession)))
                .andExpect(status().isForbidden());
    }

    @Test
    void otherModules_shouldNotRequireAToken() throws Exception {
        mvc.perform(get("/api/equipments")).andExpect(status().isOk());
    }

    // --- invalid input ---

    @Test
    void malformedJson_shouldReturn400() throws Exception {
        create("{bad").andExpect(status().isBadRequest());
    }

    @Test
    void missingFields_shouldBeReportedOneByOne() throws Exception {
        create("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email is required"))
                .andExpect(jsonPath("$.fieldErrors.role").value("Role is required"))
                .andExpect(jsonPath("$.fieldErrors.type").exists())
                .andExpect(jsonPath("$.fieldErrors.level").exists())
                .andExpect(jsonPath("$.fieldErrors.validUntil").exists());
    }

    @Test
    void tooLongValues_shouldReturn400() throws Exception {
        create("{\"name\":\"" + "n".repeat(101) + "\",\"role\":\"r\",\"type\":\"Staff\","
                + "\"level\":\"open\",\"validUntil\":\"Permanent\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
        create("{\"name\":\"n\",\"role\":\"" + "r".repeat(101) + "\",\"type\":\"Staff\","
                + "\"level\":\"" + "l".repeat(51) + "\",\"validUntil\":\"Permanent\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.role").exists())
                .andExpect(jsonPath("$.fieldErrors.level").exists());
    }
}

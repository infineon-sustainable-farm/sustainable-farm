package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.ZoneEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.ZoneRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-access;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityAccessIntegrationTest {


    private static final String BASE = "/api/v1/sitesecurity";
    private static final int CONCURRENT_REQUESTS = 16;

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

    @Autowired
    private CredentialUserRepository credentials;

    @Autowired
    private ZoneRepository zones;

    @BeforeEach
    void resetData() {
        credentials.deleteAll();
        for (String id : List.of("open", "controlled", "restricted", "critical")) {
            zones.save(ZoneEntity.builder().id(id).name(id).build());
        }
    }

    private String json(String name, String type, String level, String validUntil) {
        return "{\"name\":\"" + name + "\",\"email\":\"tester@example.test\",\"role\":\"Tester\",\"type\":\"" + type
                + "\",\"level\":\"" + level + "\",\"validUntil\":\"" + validUntil + "\"}";
    }

    private ResultActions create(String session, String body) throws Exception {
        return mvc.perform(post(BASE + "/credentials")
                .with(signedIn(session))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String futureDate() {
        return LocalDate.now().plusMonths(1).toString();
    }

    // --- authentication ---

    @Test
    void everyEndpoint_shouldRejectAnonymousCalls() throws Exception {
        for (String path : List.of("/session", "/overview", "/zones", "/credentials", "/logs")) {
            mvc.perform(get(BASE + path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Sign in to use Site Security."));
        }
        mvc.perform(post(BASE + "/credentials").contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.ORIGIN, TestSessions.APP_ORIGIN)
                        .content(json("Anon", "Staff", "critical", "Permanent")))
                .andExpect(status().isUnauthorized());
        assertEquals(0, credentials.count());
    }

    @Test
    void unknownSessionOrToken_shouldBeRejected() throws Exception {
        mvc.perform(get(BASE + "/logs").header(HttpHeaders.AUTHORIZATION, "Bearer wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/logs").with(signedIn("not-a-session")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void session_shouldReportTheRoleAndEmailOfTheOperator() throws Exception {
        mvc.perform(get(BASE + "/session").with(signedIn(readSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("read"))
                .andExpect(jsonPath("$.email").value("session-reader@example.test"));
        mvc.perform(get(BASE + "/session").with(signedIn(adminSession)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("admin"));
    }

    @Test
    void corsPreflight_shouldPassWithoutAToken() throws Exception {
        mvc.perform(options(BASE + "/credentials")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk());
    }

    // --- authorization ---

    @Test
    void readToken_shouldReadButNotCreate() throws Exception {
        for (String path : List.of("/overview", "/zones", "/credentials", "/logs")) {
            mvc.perform(get(BASE + path).with(signedIn(readSession)))
                    .andExpect(status().isOk());
        }
        create(readSession, json("Reader", "Staff", "open", futureDate()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This action requires the admin role."));
        assertEquals(0, credentials.count());
    }

    @Test
    void adminToken_shouldGrantCriticalAndPermanentAccess() throws Exception {
        create(adminSession, json("Head of Security", "Security", "critical", "Permanent"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.level").value("critical"))
                .andExpect(jsonPath("$.validUntil").value("Permanent"));
    }

    // --- server-controlled fields and business rules ---

    @Test
    void clientCannotChooseStatusOrInitials() throws Exception {
        String body = "{\"name\":\"Ada Byron\",\"email\":\"ada@example.test\",\"role\":\"Tester\",\"type\":\"Staff\",\"level\":\"open\","
                + "\"validUntil\":\"Permanent\",\"status\":\"Expired\",\"initials\":\"" + "A".repeat(300) + "\"}";

        create(adminSession, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("Active"))
                .andExpect(jsonPath("$.initials").value("AB"));
    }

    @Test
    void visitor_shouldBeLimitedToDatedOpenOrControlledAccess() throws Exception {
        create(adminSession, json("V1", "Visitor", "critical", futureDate()))
                .andExpect(status().isUnprocessableContent());
        create(adminSession, json("V2", "Visitor", "restricted", futureDate()))
                .andExpect(status().isUnprocessableContent());
        create(adminSession, json("V3", "Visitor", "open", "Permanent"))
                .andExpect(status().isUnprocessableContent());
        create(adminSession, json("V4", "Visitor", "controlled", futureDate()))
                .andExpect(status().isCreated());
        assertEquals(1, credentials.count());
    }

    // --- integrity ---

    @Test
    void duplicateName_shouldReturnConflict() throws Exception {
        create(adminSession, json("Same Name", "Staff", "open", "Permanent")).andExpect(status().isCreated());

        create(adminSession, json("Same Name", "Staff", "open", "Permanent"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A credential for 'Same Name' already exists."));
    }

    @Test
    void concurrentCreations_withDistinctNames_shouldNotLoseAnyCredential() throws Exception {
        List<Integer> statuses = createConcurrently(i -> json("Person " + i, "Staff", "open", "Permanent"));

        assertEquals(CONCURRENT_REQUESTS, statuses.stream().filter(s -> s == 201).count());
        assertEquals(CONCURRENT_REQUESTS, credentials.count());
    }

    @Test
    void concurrentCreations_withTheSameName_shouldCreateOnlyOneCredential() throws Exception {
        List<Integer> statuses = createConcurrently(i -> json("Same Person", "Staff", "open", "Permanent"));

        assertEquals(1, statuses.stream().filter(s -> s == 201).count());
        assertEquals(CONCURRENT_REQUESTS - 1, statuses.stream().filter(s -> s == 409).count());
        assertEquals(1, credentials.count());
    }

    private List<Integer> createConcurrently(IntFunction<String> bodyFor) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            String body = bodyFor.apply(i);
            results.add(executor.submit(() -> {
                start.await();
                return create(adminSession, body).andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }
        executor.shutdown();
        return statuses;
    }
}

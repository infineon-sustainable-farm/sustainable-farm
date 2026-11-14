package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AdminOnly;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.AnyAccount;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.SiteSecurityRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.config.UserPermission;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountRole;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.AccountValues.AccountStatus;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.CredentialUserEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.GateEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.LogEntryEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.entity.OperatorEntity;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.CredentialUserRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.GateRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.LogEntryRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorRepository;
import com.infineonbit.sustainablefarm.modules.sitesecurity.repository.OperatorSessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static com.infineonbit.sustainablefarm.modules.sitesecurity.TestSessions.signedIn;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Users are denied by default; each view needs its permission and shows only the user's zones. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-authz;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityAuthorizationMatrixIntegrationTest {

    private static final String BASE = "/api/v1/sitesecurity";

    @Autowired private MockMvc mvc;
    @Autowired private OperatorRepository accounts;
    @Autowired private OperatorSessionRepository sessions;
    @Autowired private GateRepository gates;
    @Autowired private CredentialUserRepository credentials;
    @Autowired private LogEntryRepository logs;
    @Autowired @Qualifier("requestMappingHandlerMapping") private RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void resetData() {
        sessions.deleteAll();
        accounts.deleteAll();
        gates.deleteAll();
        credentials.deleteAll();
        logs.deleteAll();
        gates.save(GateEntity.builder().id("g-open").name("Gate open").shortName("G1").zone("open").status("Active").build());
        gates.save(GateEntity.builder().id("g-ctrl").name("Gate controlled").shortName("G2").zone("controlled").status("Active").build());
        gates.save(GateEntity.builder().id("g-crit").name("Gate critical").shortName("G3").zone("critical").status("Secure").build());
        credentials.save(credential("c-open", "Open Person", "open"));
        credentials.save(credential("c-ctrl", "Controlled Person", "controlled"));
        credentials.save(credential("c-crit", "Critical Person", "critical"));
        logs.save(LogEntryEntity.builder().id("l-open").zone("open").status("Approved").type("Staff").date("2026-11-14").time("08:00").build());
        logs.save(LogEntryEntity.builder().id("l-ctrl").zone("controlled").status("Denied").type("Staff").date("2026-11-14").time("09:00").build());
        logs.save(LogEntryEntity.builder().id("l-crit").zone("critical").status("Denied").type("Staff").date("2026-11-14").time("10:00").build());
    }

    private static CredentialUserEntity credential(String id, String name, String level) {
        return CredentialUserEntity.builder().id(id).name(name).email(id + "@example.test").role("Tester")
                .type("Staff").level(level).validUntil("Permanent").status("Active").initials("XX").lastActive("-").build();
    }

    private String userSession(Set<String> permissions, Set<String> zones) {
        OperatorEntity account = OperatorEntity.builder()
                .id(UUID.randomUUID().toString())
                .email(UUID.randomUUID() + "@example.test")
                .fullName("Some User")
                .category("WORKER")
                .createdBy("test")
                .createdAt(Instant.now())
                .permissions(new HashSet<>(permissions))
                .zoneIds(new HashSet<>(zones))
                .build();
        account.applyRole(AccountRole.USER);
        account.applyStatus(AccountStatus.ACTIVE);
        accounts.save(account);
        return TestSessions.openFor(sessions, account.getId(), Duration.ofHours(8));
    }

    @Test
    void userWithoutPermissions_shouldOnlyReachItsOwnSession() throws Exception {
        String session = userSession(Set.of(), Set.of("open"));

        mvc.perform(get(BASE + "/session").with(signedIn(session))).andExpect(status().isOk());
        for (String path : new String[] {"/overview", "/zones", "/credentials", "/logs", "/users", "/users/summary",
                "/settings/code-sign-in"}) {
            mvc.perform(get(BASE + path).with(signedIn(session))).andExpect(status().isForbidden());
        }
        mvc.perform(post(BASE + "/credentials").with(signedIn(session)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void eachViewPermission_shouldOpenItsViewOnly_restrictedToTheUsersZones() throws Exception {
        String zonesOnly = userSession(Set.of("VIEW_ZONES"), Set.of("open"));
        mvc.perform(get(BASE + "/zones").with(signedIn(zonesOnly)))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value("open"));
        mvc.perform(get(BASE + "/logs").with(signedIn(zonesOnly))).andExpect(status().isForbidden());

        String controlled = userSession(Set.of("VIEW_DASHBOARD", "VIEW_ZONES", "VIEW_ACCESS_LOG", "VIEW_ACCESS_REGISTER"),
                Set.of("controlled"));
        mvc.perform(get(BASE + "/logs").with(signedIn(controlled)))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value("l-ctrl"));
        mvc.perform(get(BASE + "/credentials").with(signedIn(controlled)))
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].name").value("Controlled Person"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get(BASE + "/overview").with(signedIn(controlled)))
                .andExpect(jsonPath("$.gates", hasSize(1))).andExpect(jsonPath("$.gates[0].id").value("g-ctrl"))
                .andExpect(jsonPath("$.stats.alerts").value(1));
        // Reading never allows writing.
        mvc.perform(post(BASE + "/credentials/c-ctrl/revoke").with(signedIn(controlled))).andExpect(status().isForbidden());
    }

    @Test
    void userWithAPermissionButNoZone_shouldSeeNothing() throws Exception {
        String session = userSession(Set.of("VIEW_ZONES", "VIEW_ACCESS_LOG"), Set.of());

        mvc.perform(get(BASE + "/zones").with(signedIn(session))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(BASE + "/logs").with(signedIn(session))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void legacyReadOnlyOperator_shouldKeepEveryViewAndZone_butNoWrites() throws Exception {
        String session = TestSessions.open(accounts, sessions, "legacy.reader@example.test", SiteSecurityRole.READ);

        mvc.perform(get(BASE + "/zones").with(signedIn(session))).andExpect(jsonPath("$", hasSize(4)));
        mvc.perform(get(BASE + "/logs").with(signedIn(session))).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get(BASE + "/credentials").with(signedIn(session)))
                .andExpect(jsonPath("$", hasSize(3))).andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get(BASE + "/users").with(signedIn(session))).andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/credentials/c-open/revoke").with(signedIn(session))).andExpect(status().isForbidden());
    }

    /** Fails when an endpoint becomes reachable by users without being reviewed here. */
    @Test
    void theOnlyEndpointsUsersCanReach_shouldBeTheseOnes() {
        Set<String> reachable = new TreeSet<>();
        for (var entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod method = entry.getValue();
            if (!method.getBeanType().getPackageName().contains(".sitesecurity.")) {
                continue;
            }
            RequestMappingInfo info = entry.getKey();
            boolean adminOnly = method.getBeanType().isAnnotationPresent(AdminOnly.class);
            boolean any = method.hasMethodAnnotation(AnyAccount.class) || method.getBeanType().isAnnotationPresent(AnyAccount.class);
            boolean permission = method.hasMethodAnnotation(UserPermission.class);
            if (!adminOnly && (any || permission)) {
                reachable.add(info.getMethodsCondition().getMethods() + " " + info.getPathPatternsCondition().getPatternValues());
            }
        }
        assertEquals(new TreeSet<>(Set.of(
                "[GET] [/api/v1/sitesecurity/session]",
                "[GET] [/api/v1/sitesecurity/overview]",
                "[GET] [/api/v1/sitesecurity/zones]",
                "[GET] [/api/v1/sitesecurity/credentials]",
                "[GET] [/api/v1/sitesecurity/logs]",
                "[POST] [/api/v1/sitesecurity/account/password]",
                "[POST] [/api/v1/sitesecurity/account/password/setup]")), reachable);
    }
}

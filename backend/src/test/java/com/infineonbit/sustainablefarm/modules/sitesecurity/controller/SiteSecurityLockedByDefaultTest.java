package com.infineonbit.sustainablefarm.modules.sitesecurity.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A deployment that forgets its settings must stay closed, not fall open. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-locked;DB_CLOSE_DELAY=-1",
        "SITESECURITY_GATE_TOKEN=",
        // The tokens of the previous version must not open anything any more.
        "SITESECURITY_READ_TOKEN=" + SiteSecurityLockedByDefaultTest.OLD_TOKEN,
        "SITESECURITY_ADMIN_TOKEN=" + SiteSecurityLockedByDefaultTest.OLD_TOKEN
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityLockedByDefaultTest {

    static final String OLD_TOKEN = "old-token-from-the-previous-version-0123456789";

    @Autowired
    private MockMvc mvc;

    @Test
    void api_shouldRefuseEveryCaller_withoutASession() throws Exception {
        for (String header : List.of("Bearer ", "Bearer null", "Bearer undefined", "Bearer " + "a".repeat(64),
                "Bearer " + OLD_TOKEN)) {
            mvc.perform(get("/api/v1/sitesecurity/logs").header(HttpHeaders.AUTHORIZATION, header))
                    .andExpect(status().isUnauthorized());
        }
    }
}

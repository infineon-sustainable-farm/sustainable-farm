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

/** A deployment that forgets its tokens must stay closed, not fall open. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:sitesecurity-locked;DB_CLOSE_DELAY=-1",
        "SITESECURITY_READ_TOKEN=",
        "SITESECURITY_ADMIN_TOKEN="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SiteSecurityLockedByDefaultTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void api_shouldRefuseEveryCaller_whenNoTokenIsConfigured() throws Exception {
        for (String header : List.of("Bearer ", "Bearer null", "Bearer undefined", "Bearer " + "a".repeat(64))) {
            mvc.perform(get("/api/v1/sitesecurity/logs").header(HttpHeaders.AUTHORIZATION, header))
                    .andExpect(status().isUnauthorized());
        }
    }
}

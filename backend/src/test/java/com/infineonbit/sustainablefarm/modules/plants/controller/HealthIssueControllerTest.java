package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthIssueService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HealthIssueController.class)
@Import(CoreExceptionHandler.class)
public class HealthIssueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthIssueService healthIssueService;

    @Test
    void getAllIssues_shouldReturnTheCatalogue() throws Exception {
        // Arrange
        when(healthIssueService.getAllIssues()).thenReturn(List.of(
                new HealthIssueResponse(5L, "ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE,
                        "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025"),
                new HealthIssueResponse(8L, "OTHER", "Other", HealthIssueKind.OTHER, null, null, "by_definition")));
        // Act & Assert
        mockMvc.perform(get("/api/plants/health-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].code").value("ANTHRACNOSE"))
                .andExpect(jsonPath("$[0].name").value("Anthracnose"))
                .andExpect(jsonPath("$[0].kind").value("DISEASE"))
                .andExpect(jsonPath("$[0].scientificName").value("Colletotrichum gloeosporioides"))
                .andExpect(jsonPath("$[0].eppoCode").value("COLLGL"))
                .andExpect(jsonPath("$[0].source").value("Dianda_2025"))
                .andExpect(jsonPath("$[1].code").value("OTHER"))
                .andExpect(jsonPath("$[1].eppoCode").value(nullValue()));
    }
}

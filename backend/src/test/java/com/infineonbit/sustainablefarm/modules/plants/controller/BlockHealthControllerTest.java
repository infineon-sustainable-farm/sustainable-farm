package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.BlockHealthResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthCategory;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthInspectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BlockHealthController.class)
@Import(CoreExceptionHandler.class)
public class BlockHealthControllerTest {

    private static final String URL = "/api/plants/block-health";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthInspectionService healthInspectionService;

    @Test
    void getBlockHealth_shouldReturnEachBlock_andPassTheFarmFilter() throws Exception {
        // Arrange
        when(healthInspectionService.getBlockHealth(null)).thenReturn(List.of(
                new BlockHealthResponse(null, "C", 4L, LocalDate.of(2026, 9, 21), 72, HealthCategory.HEALTHY),
                new BlockHealthResponse(null, "D", 2L, LocalDate.of(2026, 9, 2), 85, HealthCategory.VERY_HEALTHY)));
        when(healthInspectionService.getBlockHealth(1)).thenReturn(List.of());
        // Act & Assert: every farm
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].farmId").value(nullValue()))
                .andExpect(jsonPath("$[0].blockCode").value("C"))
                .andExpect(jsonPath("$[0].inspectionId").value(4))
                .andExpect(jsonPath("$[0].inspectedOn").value("2026-09-21"))
                .andExpect(jsonPath("$[0].healthScorePct").value(72))
                .andExpect(jsonPath("$[0].healthCategory").value("HEALTHY"))
                .andExpect(jsonPath("$[1].healthCategory").value("VERY_HEALTHY"));
        // Act & Assert: farm 1
        mockMvc.perform(get(URL).param("farmId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}

package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.exception.VarietyNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.VarietyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VarietyController.class)
@Import(CoreExceptionHandler.class)
public class VarietyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VarietyService varietyService;

    @Test
    void getVarietyById_shouldReturn404WithApiError_whenIdDoesNotExist() throws Exception {
        // Arrange
        when(varietyService.getVarietyById(999L)).thenThrow(new VarietyNotFoundException(999L));
        // Act
        ResultActions result = mockMvc.perform(get("/api/plants/varieties/999"));
        // Assert: the application-wide ApiError body, not a bare string
        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Variety with ID 999 not found"))
                .andExpect(jsonPath("$.path").value("uri=/api/plants/varieties/999"))
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

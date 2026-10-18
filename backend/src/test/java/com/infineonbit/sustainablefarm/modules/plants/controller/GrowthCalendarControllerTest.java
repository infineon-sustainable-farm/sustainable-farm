package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.exception.GrowthCalendarNotFoundException;
import com.infineonbit.sustainablefarm.modules.plants.service.GrowthCalendarService;
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

@WebMvcTest(GrowthCalendarController.class)
@Import(CoreExceptionHandler.class)
public class GrowthCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GrowthCalendarService growthCalendarService;

    @Test
    void getGrowthCalendarEntryById_shouldReturn404WithApiError_whenIdDoesNotExist() throws Exception {
        // Arrange
        when(growthCalendarService.getGrowthCalendarEntryById(999L))
                .thenThrow(new GrowthCalendarNotFoundException(999L));
        // Act
        ResultActions result = mockMvc.perform(get("/api/plants/growth-calendar/999"));
        // Assert: the application-wide ApiError body, not a bare string
        result.andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Growth calendar entry with ID 999 not found"))
                .andExpect(jsonPath("$.path").value("uri=/api/plants/growth-calendar/999"))
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

package com.infineonbit.sustainablefarm.modules.plants.controller;

import com.infineonbit.sustainablefarm.core.exception.ConflictException;
import com.infineonbit.sustainablefarm.core.exception.CoreExceptionHandler;
import com.infineonbit.sustainablefarm.modules.plants.dto.Request.HealthIssueRequest;
import com.infineonbit.sustainablefarm.modules.plants.dto.Response.HealthIssueResponse;
import com.infineonbit.sustainablefarm.modules.plants.entity.HealthIssueKind;
import com.infineonbit.sustainablefarm.modules.plants.service.HealthIssueService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The error bodies are asserted in full, as for the fertilizer routes. */
@WebMvcTest(HealthIssueController.class)
@Import(CoreExceptionHandler.class)
public class HealthIssueControllerTest {

    private static final String URL = "/api/plants/health-issues";
    private static final String NO_CODE_MESSAGE = "name must contain a letter from A to Z or a digit";
    private static final String OTHER_MESSAGE =
            "name must not give the code OTHER, which is reserved for problems outside the catalogue";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthIssueService healthIssueService;

    private ResultActions postIssue(String body) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Common part of every 4xx body: the application-wide ApiError envelope. */
    private static void assertApiError(ResultActions result, int status, String error, String message)
            throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.path").value("uri=" + URL))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private void assertRefused(ResultActions result, String field, String message) throws Exception {
        assertApiError(result, 400, "Bad Request", "Validation failed");
        result.andExpect(jsonPath("$.fieldErrors", aMapWithSize(1)))
                .andExpect(jsonPath("$.fieldErrors." + field).value(message));
        verify(healthIssueService, never()).createIssue(any());
    }

    private void assertNameRefused(String name, String message) throws Exception {
        assertRefused(postIssue("""
                {"name":"%s","kind":"DISEASE"}
                """.formatted(name)), "name", message);
    }

    @Test
    void getAllIssues_shouldReturnTheCatalogue() throws Exception {
        // Arrange
        when(healthIssueService.getAllIssues()).thenReturn(List.of(
                new HealthIssueResponse(5L, "ANTHRACNOSE", "Anthracnose", HealthIssueKind.DISEASE,
                        "Colletotrichum gloeosporioides", "COLLGL", "Dianda_2025"),
                new HealthIssueResponse(8L, "OTHER", "Other", HealthIssueKind.OTHER, null, null, "by_definition")));
        // Act & Assert
        mockMvc.perform(get(URL))
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

    @Test
    void createIssue_shouldReturn201WithTheAddedIssue() throws Exception {
        // Arrange
        when(healthIssueService.createIssue(any(HealthIssueRequest.class))).thenReturn(new HealthIssueResponse(9L,
                "POWDERY_MILDEW", "Powdery mildew", HealthIssueKind.DISEASE, null, null, "user_entry"));
        // Act
        ResultActions result = postIssue("""
                {"name":"Powdery mildew","kind":" disease "}
                """);
        // Assert: the shape of the catalogue, and the request as sent
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$", aMapWithSize(7)))
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.code").value("POWDERY_MILDEW"))
                .andExpect(jsonPath("$.name").value("Powdery mildew"))
                .andExpect(jsonPath("$.kind").value("DISEASE"))
                .andExpect(jsonPath("$.scientificName").value(nullValue()))
                .andExpect(jsonPath("$.eppoCode").value(nullValue()))
                .andExpect(jsonPath("$.source").value("user_entry"));
        ArgumentCaptor<HealthIssueRequest> captor = ArgumentCaptor.forClass(HealthIssueRequest.class);
        verify(healthIssueService).createIssue(captor.capture());
        assertEquals(new HealthIssueRequest("Powdery mildew", " disease ", null, null, null), captor.getValue());
    }

    @Test
    void createIssue_shouldReturn400_whenTheNameIsMissing() throws Exception {
        assertRefused(postIssue("""
                {"kind":"PEST"}
                """), "name", "name is required");
    }

    @Test
    void createIssue_shouldReturn400WithOneMessage_whenTheNameIsBlankAndTooLong() throws Exception {
        assertNameRefused(" ".repeat(60), "name is required");
    }

    @Test
    void createIssue_shouldReturn400_whenTheNameIsLongerThan50Characters() throws Exception {
        // Act & Assert: 50 characters once trimmed pass, 51 do not
        assertNameRefused("m".repeat(51), "name must be at most 50 characters");
    }

    @Test
    void createIssue_shouldReturn400_whenTheNameGivesNoCode() throws Exception {
        assertNameRefused("!!!", NO_CODE_MESSAGE);
        assertNameRefused("木瓜", NO_CODE_MESSAGE);
    }

    @Test
    void createIssue_shouldReturn400_whenTheNameGivesOther() throws Exception {
        assertNameRefused("Other", OTHER_MESSAGE);
        assertNameRefused(" other! ", OTHER_MESSAGE);
        assertNameRefused("Ôther", OTHER_MESSAGE);
    }

    @Test
    void createIssue_shouldReturn400_whenTheKindIsMissing() throws Exception {
        assertRefused(postIssue("""
                {"name":"Termites"}
                """), "kind", "kind is required");
    }

    @Test
    void createIssue_shouldReturn400_whenTheKindIsOther() throws Exception {
        assertRefused(postIssue("""
                {"name":"Termites","kind":"OTHER"}
                """), "kind", "kind must be PEST or DISEASE");
    }

    @Test
    void createIssue_shouldReturn400_whenTheEppoCodeIsNotLettersAndDigits() throws Exception {
        assertRefused(postIssue("""
                {"name":"Anthracnose","kind":"DISEASE","eppoCode":"COLL-GL"}
                """), "eppoCode", "eppoCode must be 1 to 10 letters or digits");
        assertRefused(postIssue("""
                {"name":"Anthracnose","kind":"DISEASE","eppoCode":"COLLGLCOLLG"}
                """), "eppoCode", "eppoCode must be 1 to 10 letters or digits");
    }

    @Test
    void createIssue_shouldReturn400_whenTheScientificNameOrTheSourceIsTooLong() throws Exception {
        assertRefused(postIssue("""
                {"name":"Anthracnose","kind":"DISEASE","scientificName":"%s"}
                """.formatted("s".repeat(256))), "scientificName", "scientificName must be at most 255 characters");
        assertRefused(postIssue("""
                {"name":"Anthracnose","kind":"DISEASE","source":"%s"}
                """.formatted("s".repeat(256))), "source", "source must be at most 255 characters");
    }

    @Test
    void createIssue_shouldReturn409WithApiError_whenTheNameIsAlreadyInTheCatalogue() throws Exception {
        // Arrange
        when(healthIssueService.createIssue(any(HealthIssueRequest.class)))
                .thenThrow(new ConflictException("A health issue named Powdery mildew already exists"));
        // Act
        ResultActions result = postIssue("""
                {"name":" POWDERY MILDEW ","kind":"disease"}
                """);
        // Assert
        assertApiError(result, 409, "Conflict", "A health issue named Powdery mildew already exists");
        result.andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }
}

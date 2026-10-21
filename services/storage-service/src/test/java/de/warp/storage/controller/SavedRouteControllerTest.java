package de.warp.storage.controller;

import de.warp.storage.SavedRouteTestData;
import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.SavedRouteResponse;
import de.warp.storage.exceptions.SavedRouteNotFoundException;
import de.warp.storage.filter.InternalServiceAuthenticationFilter;
import de.warp.storage.service.SavedRouteCreationResult;
import de.warp.storage.service.SavedRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = SavedRouteController.class,
        properties = {
                "warp.security.internal-service-token="
                        + "test-internal-service-token"
        }
)
class SavedRouteControllerTest {

    private static final String BASE =
            "/internal/v1/users/usr_123/saved-routes";

    private static final String TOKEN = "test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private SavedRouteService savedRouteService;

    @Test
    void shouldRejectRequestsWithoutServiceToken() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVICE_UNAUTHORIZED"));
    }

    @Test
    void shouldCreateRoute() throws Exception {
        CreateSavedRouteRequest request = SavedRouteTestData.sampleRequest();

        SavedRouteResponse response = new SavedRouteResponse(
                "route_123",
                request.name(),
                false,
                request.routeSnapshot(),
                request.weatherSnapshot(),
                OffsetDateTime.parse("2026-10-21T10:00:00Z"),
                OffsetDateTime.parse("2026-10-21T10:00:00Z")
        );

        when(savedRouteService.create(
                eq("usr_123"), eq("key-1"), any(CreateSavedRouteRequest.class)
        )).thenReturn(new SavedRouteCreationResult(response, true));

        mockMvc.perform(
                        post(BASE)
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                                .header("Idempotency-Key", "key-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("route_123"))
                .andExpect(jsonPath("$.weatherSnapshot.overallScore")
                        .value(78));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        CreateSavedRouteRequest valid = SavedRouteTestData.sampleRequest();
        CreateSavedRouteRequest invalid = new CreateSavedRouteRequest(
                "   ",
                false,
                valid.routeSnapshot(),
                valid.weatherSnapshot()
        );

        mockMvc.perform(
                        post(BASE)
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonMapper.writeValueAsString(invalid))
                )
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_SAVED_ROUTE_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("name"));
    }

    @Test
    void shouldReturnNotFoundForUnknownOrForeignRoute() throws Exception {
        when(savedRouteService.get("usr_123", "route_x"))
                .thenThrow(new SavedRouteNotFoundException());

        mockMvc.perform(
                        get(BASE + "/route_x")
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SAVED_ROUTE_NOT_FOUND"));
    }

    @Test
    void shouldDeleteRoute() throws Exception {
        mockMvc.perform(
                        delete(BASE + "/route_1")
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingForeignRoute() throws Exception {
        doThrow(new SavedRouteNotFoundException())
                .when(savedRouteService).delete("usr_123", "route_1");

        mockMvc.perform(
                        delete(BASE + "/route_1")
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                )
                .andExpect(status().isNotFound());
    }
}
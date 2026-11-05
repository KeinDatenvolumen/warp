// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.storage.controller;

import de.warp.storage.SavedRouteTestData;
import de.warp.storage.dto.CreateSavedRouteRequest;
import de.warp.storage.dto.GeoJsonLineString;
import de.warp.storage.dto.RouteSnapshotDto;
import de.warp.storage.dto.SavedRouteResponse;
import de.warp.storage.dto.WeatherSnapshotDto;
import de.warp.storage.exceptions.SavedRouteNotFoundException;
import de.warp.storage.filter.InternalServiceAuthenticationFilter;
import de.warp.storage.filter.UserContextFilter;
import de.warp.storage.service.SavedRouteCreationResult;
import de.warp.storage.service.SavedRouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private static final String USER_ID = "usr_123";

    private static final String BASE =
            "/internal/v1/users/" + USER_ID + "/saved-routes";

    private static final String TOKEN =
            "test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private SavedRouteService savedRouteService;

    private static MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder builder
    ) {
        return builder
                .header(
                        InternalServiceAuthenticationFilter.HEADER_NAME,
                        TOKEN
                )
                .header(
                        UserContextFilter.HEADER_NAME,
                        USER_ID
                );
    }

    @Test
    void shouldRejectRequestsWithoutServiceToken() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVICE_UNAUTHORIZED"));
    }

    @Test
    void shouldRejectRequestsWithoutUserContextHeader() throws Exception {
        mockMvc.perform(
                        get(BASE)
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("USER_CONTEXT_MISMATCH"));
    }

    @Test
    void shouldRejectUserContextThatDoesNotMatchPath() throws Exception {
        mockMvc.perform(
                        get(BASE)
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        TOKEN
                                )
                                .header(
                                        UserContextFilter.HEADER_NAME,
                                        "usr_other"
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("USER_CONTEXT_MISMATCH"));
    }

    @Test
    void shouldAcceptContractExampleWithEmptyArrays() throws Exception {
        CreateSavedRouteRequest baseRequest =
                SavedRouteTestData.sampleRequest();

        RouteSnapshotDto route = baseRequest.routeSnapshot();
        WeatherSnapshotDto weather = baseRequest.weatherSnapshot();

        CreateSavedRouteRequest request = new CreateSavedRouteRequest(
                baseRequest.name(),
                false,
                new RouteSnapshotDto(
                        route.routeCalculationId(),
                        route.start(),
                        route.destination(),
                        route.activityProfileCode(),
                        route.departureTime(),
                        route.arrivalTime(),
                        route.distanceMeters(),
                        route.durationSeconds(),
                        new GeoJsonLineString(
                                "LineString",
                                List.of()
                        )
                ),
                new WeatherSnapshotDto(
                        weather.weatherEvaluationId(),
                        weather.overallScore(),
                        weather.scoreVersion(),
                        weather.thunderstormCapApplied(),
                        List.of()
                )
        );

        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-10-21T10:00:00Z");

        SavedRouteResponse response = new SavedRouteResponse(
                "route_123",
                request.name(),
                false,
                request.routeSnapshot(),
                request.weatherSnapshot(),
                timestamp,
                timestamp
        );

        when(savedRouteService.create(
                eq(USER_ID),
                eq("key-1"),
                any(CreateSavedRouteRequest.class)
        )).thenReturn(new SavedRouteCreationResult(response, true));

        mockMvc.perform(
                        authenticated(post(BASE))
                                .header("Idempotency-Key", "key-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value("route_123"));
    }

    @Test
    void shouldCreateRoute() throws Exception {
        CreateSavedRouteRequest request =
                SavedRouteTestData.sampleRequest();

        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-10-21T10:00:00Z");

        SavedRouteResponse response = new SavedRouteResponse(
                "route_123",
                request.name(),
                false,
                request.routeSnapshot(),
                request.weatherSnapshot(),
                timestamp,
                timestamp
        );

        when(savedRouteService.create(
                eq(USER_ID),
                eq("key-1"),
                any(CreateSavedRouteRequest.class)
        )).thenReturn(new SavedRouteCreationResult(response, true));

        mockMvc.perform(
                        authenticated(post(BASE))
                                .header("Idempotency-Key", "key-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id")
                        .value("route_123"))
                .andExpect(jsonPath("$.weatherSnapshot.overallScore")
                        .value(78));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        CreateSavedRouteRequest valid =
                SavedRouteTestData.sampleRequest();

        CreateSavedRouteRequest invalid =
                new CreateSavedRouteRequest(
                        "   ",
                        false,
                        valid.routeSnapshot(),
                        valid.weatherSnapshot()
                );

        mockMvc.perform(
                        authenticated(post(BASE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        jsonMapper.writeValueAsString(invalid)
                                )
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_SAVED_ROUTE_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("name"));
    }

    @Test
    void shouldReturnNotFoundForUnknownOrForeignRoute()
            throws Exception {
        when(savedRouteService.get(USER_ID, "route_x"))
                .thenThrow(new SavedRouteNotFoundException());

        mockMvc.perform(
                        authenticated(get(BASE + "/route_x"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SAVED_ROUTE_NOT_FOUND"));
    }

    @Test
    void shouldDeleteRoute() throws Exception {
        mockMvc.perform(
                        authenticated(delete(BASE + "/route_1"))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingForeignRoute()
            throws Exception {
        doThrow(new SavedRouteNotFoundException())
                .when(savedRouteService)
                .delete(USER_ID, "route_1");

        mockMvc.perform(
                        authenticated(delete(BASE + "/route_1"))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("SAVED_ROUTE_NOT_FOUND"));
    }

    @Test
    void shouldRejectPatchWithEmptyBody() throws Exception {
        when(savedRouteService.update(
                eq(USER_ID),
                eq("route_1"),
                any()
        )).thenThrow(
                new de.warp.storage.exceptions
                        .InvalidSavedRouteRequestException(
                        "body",
                        "AT_LEAST_ONE_FIELD_REQUIRED",
                        "Mindestens ein Feld muss geändert werden."
                )
        );

        mockMvc.perform(
                        authenticated(patch(BASE + "/route_1"))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_SAVED_ROUTE_REQUEST"));
    }

    @Test
    void shouldRejectInvalidCoordinates() throws Exception {
        String invalidCoordsJson = """
                {
                  "name": "Ungültige Route",
                  "favorite": false,
                  "routeSnapshot": {
                    "routeCalculationId": "calc_1",
                    "start": {
                      "label": "Start",
                      "latitude": 95.0,
                      "longitude": 9.0
                    },
                    "destination": {
                      "label": "Ziel",
                      "latitude": 49.0,
                      "longitude": 9.0
                    },
                    "activityProfileCode": "BICYCLE",
                    "departureTime": "2026-10-24T08:00:00Z",
                    "arrivalTime": "2026-10-24T09:00:00Z",
                    "distanceMeters": 10000,
                    "durationSeconds": 3600,
                    "geometry": {
                      "type": "LineString",
                      "coordinates": [
                        [9.0, 49.0],
                        [9.0, 49.5]
                      ]
                    }
                  },
                  "weatherSnapshot": {
                    "weatherEvaluationId": "weather_1",
                    "overallScore": 80,
                    "scoreVersion": "1.0",
                    "thunderstormCapApplied": false,
                    "segments": []
                  }
                }
                """;

        mockMvc.perform(
                        authenticated(post(BASE))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidCoordsJson)
                )
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_SAVED_ROUTE_REQUEST"));
    }
}
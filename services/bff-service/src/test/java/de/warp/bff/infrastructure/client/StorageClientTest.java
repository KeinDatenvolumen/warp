// [KI-Log Nr. KI-13] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Persistenz gespeicherter Routen (WARP-020/021)
package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;
import de.warp.bff.application.exception.StorageValidationException;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.CreateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.GeoJsonLineString;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.Place;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.RouteSnapshot;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoute;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.StorageFieldError;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.UpdateSavedRouteRequest;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.WeatherSnapshot;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StorageClientTest {

    private static final String BASE = "http://storage-service";

    private MockRestServiceServer mockServer;
    private StorageClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new StorageClient(builder.build());
    }

    @Test
    void shouldListRoutesOfUser() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes"
                                + "?page=0&size=20&favorite=true"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-User-Id", "usr_123"))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [],
                          "page": 0,
                          "size": 20,
                          "totalElements": 0,
                          "totalPages": 0
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SavedRoutePage page = client.listSavedRoutes("usr_123", true, 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void shouldTranslateNotFound() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(SavedRouteNotFoundException.class);
    }

    @Test
    void shouldTranslateServerErrorToUnavailable() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(StorageServiceUnavailableException.class);
    }

    @Test
    void shouldDeleteRoute() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_1"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header("X-User-Id", "usr_123"))
                .andRespond(withNoContent());

        client.deleteSavedRoute("usr_123", "route_1");

        mockServer.verify();
    }

    @Test
    void shouldTranslateBadRequest() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldTranslateUnauthorizedToUnavailable() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(StorageServiceUnavailableException.class);
    }

    @Test
    void shouldTranslateForbiddenToNotFound() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(SavedRouteNotFoundException.class);
    }

    @Test
    void shouldCreateSavedRoute() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-User-Id", "usr_123"))
                .andExpect(header("Idempotency-Key", "idemp-key-1"))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "id": "route_1",
                                  "name": "Meine Tour",
                                  "favorite": false,
                                  "routeSnapshot": {
                                    "routeCalculationId": "calc_1",
                                    "start": {"label": "A", "latitude": 49.0, "longitude": 9.0},
                                    "destination": {"label": "B", "latitude": 49.5, "longitude": 9.5},
                                    "activityProfileCode": "BICYCLE",
                                    "departureTime": "2026-10-24T08:00:00Z",
                                    "arrivalTime": "2026-10-24T09:00:00Z",
                                    "distanceMeters": 10000,
                                    "durationSeconds": 3600,
                                    "geometry": {"type": "LineString", "coordinates": [[9.0, 49.0], [9.5, 49.5]]}
                                  },
                                  "weatherSnapshot": {
                                    "weatherEvaluationId": "weather_1",
                                    "overallScore": 80,
                                    "scoreVersion": "1.0",
                                    "thunderstormCapApplied": false,
                                    "segments": []
                                  },
                                  "createdAt": "2026-10-24T08:00:00Z",
                                  "updatedAt": "2026-10-24T08:00:00Z"
                                }
                                """));

        OffsetDateTime now = OffsetDateTime.parse("2026-10-24T08:00:00Z");
        CreateSavedRouteRequest request = new CreateSavedRouteRequest(
                "Meine Tour",
                false,
                new RouteSnapshot(
                        "calc_1",
                        new Place("A", 49.0, 9.0),
                        new Place("B", 49.5, 9.5),
                        "BICYCLE",
                        now,
                        now.plusHours(1),
                        10000,
                        3600,
                        new GeoJsonLineString("LineString", List.of(List.of(9.0, 49.0), List.of(9.5, 49.5)))
                ),
                new WeatherSnapshot("weather_1", 80, "1.0", false, List.of())
        );

        SavedRoute created = client.createSavedRoute("usr_123", "idemp-key-1", request);

        assertThat(created.id()).isEqualTo("route_1");
        assertThat(created.name()).isEqualTo("Meine Tour");
    }

    @Test
    void shouldTranslateValidationErrorWithFieldErrors() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_1"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(header("X-User-Id", "usr_123"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {
                                  "code": "INVALID_SAVED_ROUTE_REQUEST",
                                  "fieldErrors": [
                                    {"field": "name", "code": "INVALID_NAME",
                                     "message": "Der Routenname darf nicht leer sein."}
                                  ]
                                }
                                """));

        assertThatThrownBy(() -> client.updateSavedRoute(
                "usr_123", "route_1", new UpdateSavedRouteRequest("x", null)
        )).isInstanceOfSatisfying(
                StorageValidationException.class,
                exception -> assertThat(exception.getFieldErrors())
                        .extracting(StorageFieldError::field)
                        .containsExactly("name")
        );
    }
}
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.api.controller;

import de.warp.bff.api.dto.RouteWeatherAiAnalysisApiRequest;
import de.warp.bff.api.dto.RouteWeatherOptimizationApiRequest;
import de.warp.bff.application.service.RouteWeatherOptimizationService;
import de.warp.bff.application.service.SessionService;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.CriticalSectionHazardResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.DepartureSlotResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.HiddenParameterInsightResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.OptimalWindowSummaryResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import de.warp.bff.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteWeatherOptimizationController.class)
@Import(RouteWeatherOptimizationControllerTest.SecurityMvcTestConfig.class)
class RouteWeatherOptimizationControllerTest {

    @TestConfiguration
    static class SecurityMvcTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RouteWeatherOptimizationService optimizationService;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @Test
    void shouldReturnOptimizationResultWhenValidRequestProvided() throws Exception {
        DepartureSlotResponse slot1 = new DepartureSlotResponse(
                "2026-05-16T08:00:00Z",
                "08:00",
                "2026-05-16T09:12:00Z",
                "09:12",
                92,
                91.5,
                false,
                false,
                List.of(90, 92, 94),
                List.of("Trocken", "Rückenwind", "Trocken"),
                "+40 Pkt. ggü. 14:00 Uhr"
        );

        OptimalWindowSummaryResponse summary = new OptimalWindowSummaryResponse(
                "08:00",
                "2026-05-16T08:00:00Z",
                92,
                "07:30 – 09:00",
                "+40 Punkte ggü. 14:00 Uhr (optimale Wetterkonditionen)",
                "14:00",
                52
        );

        when(optimizationService.optimizeDeparture(anyString(), any(RouteWeatherOptimizationApiRequest.class))).thenReturn(
                new RouteWeatherOptimizationResponse(
                        0,
                        summary,
                        List.of(slot1)
                )
        );

        String jsonBody = """
                {
                  "routeCalculationId": "calc-123",
                  "activityProfile": "BICYCLE",
                  "departureTime": "2026-05-16T08:00:00Z",
                  "durationSeconds": 3600,
                  "distanceMeters": 24000,
                  "coordinates": [
                    [13.47, 51.16],
                    [13.43, 51.20],
                    [13.30, 51.30]
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/routes/weather-optimization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedSlotIndex").value(0))
                .andExpect(jsonPath("$.optimalWindowSummary.bestDeparture").value("08:00"))
                .andExpect(jsonPath("$.optimalWindowSummary.bestScore").value(92))
                .andExpect(jsonPath("$.departureSlots[0].routeScore").value(92))
                .andExpect(jsonPath("$.departureSlots[0].segmentScores[0]").value(90));
    }

    @Test
    void shouldReturnBadRequestWhenOptimizationServiceRejectsRequest() throws Exception {
        when(optimizationService.optimizeDeparture(anyString(), any()))
                .thenThrow(new IllegalArgumentException("Invalid input"));

        String jsonBody = """
                {
                  "activityProfile": "BICYCLE"
                }
                """;

        mockMvc.perform(post("/api/v1/routes/weather-optimization")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyMissingForOptimization() throws Exception {
        mockMvc.perform(post("/api/v1/routes/weather-optimization")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectAiAnalysisWhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();
        String jsonBody = """
                {
                  "routeCalculationId": "calc-123",
                  "activityProfile": "BICYCLE",
                  "coordinates": [[9.15, 49.35], [9.77, 49.49]]
                }
                """;

        mockMvc.perform(post("/api/v1/routes/weather-ai-analysis")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("SESSION_INVALID"));
    }

    @Test
    void shouldRejectAiAnalysisWhenEmailNotVerified() throws Exception {
        AuthenticatedSessionPrincipal unverifiedPrincipal = new AuthenticatedSessionPrincipal(
                "usr-unverified",
                Instant.parse("2026-05-16T07:00:00Z"),
                Instant.parse("2026-05-16T19:00:00Z"),
                false
        );
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(unverifiedPrincipal, null, List.of())
        );

        try {
            String jsonBody = """
                    {
                      "routeCalculationId": "calc-123",
                      "activityProfile": "BICYCLE",
                      "coordinates": [[9.15, 49.35], [9.77, 49.49]]
                    }
                    """;

            mockMvc.perform(post("/api/v1/routes/weather-ai-analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldReturnAiAnalysisForVerifiedUser() throws Exception {
        AuthenticatedSessionPrincipal verifiedPrincipal = new AuthenticatedSessionPrincipal(
                "usr-verified",
                Instant.parse("2026-05-16T07:00:00Z"),
                Instant.parse("2026-05-16T19:00:00Z"),
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(verifiedPrincipal, null, List.of())
        );

        try {
            when(optimizationService.analyzeRouteWeatherAi(anyString(), any(RouteWeatherAiAnalysisApiRequest.class))).thenReturn(
                    new RouteWeatherAiAnalysisResponse(
                            "calc-123",
                            "MOTORCYCLE",
                            "Motorrad",
                            Instant.parse("2026-05-16T08:00:00Z"),
                            74,
                            false,
                            "MODERATE",
                            "MCP-Wetteranalyse für Motorrad",
                            "Zusammenfassung der bekannten und versteckten Parameter.",
                            "Pinlock-Visier verwenden.",
                            List.of(new HiddenParameterInsightResponse(
                                    "WET_ROAD_INDEX",
                                    "Fahrbahnnässe-Index",
                                    "ROAD_SURFACE",
                                    "45 %",
                                    "NOTICE",
                                    "Restnässe in Kurven."
                            )),
                            List.of(new CriticalSectionHazardResponse(
                                    1,
                                    1,
                                    "km 10,0 – 20,0",
                                    "08:15 – 08:30 Uhr",
                                    62,
                                    "WARNING",
                                    "WIND",
                                    "Seitenwindböen",
                                    "Böen bis 42 km/h.",
                                    "Tempo anpassen."
                            )),
                            List.of("Vorsichtig fahren"),
                            List.of("get_route_weather_telemetry"),
                            "gemini-2.5-flash (MCP)",
                            Instant.parse("2026-05-16T07:05:00Z")
                    )
            );

            String jsonBody = """
                    {
                      "routeCalculationId": "calc-123",
                      "startLabel": "Mosbach",
                      "destinationLabel": "Bad Mergentheim",
                      "activityProfile": "MOTORCYCLE",
                      "departureTime": "2026-05-16T08:00:00Z",
                      "durationSeconds": 2700,
                      "distanceMeters": 35000,
                      "coordinates": [[9.15, 49.35], [9.77, 49.49]]
                    }
                    """;

            mockMvc.perform(post("/api/v1/routes/weather-ai-analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activityProfileCode").value("MOTORCYCLE"))
                    .andExpect(jsonPath("$.modelName").value("gemini-2.5-flash (MCP)"))
                    .andExpect(jsonPath("$.hiddenParameterInsights[0].parameterKey").value("WET_ROAD_INDEX"))
                    .andExpect(jsonPath("$.criticalSections[0].title").value("Seitenwindböen"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldReturnBadRequestWhenAiServiceRejectsRequestOrBodyMissing() throws Exception {
        AuthenticatedSessionPrincipal verifiedPrincipal = new AuthenticatedSessionPrincipal(
                "usr-verified",
                Instant.parse("2026-05-16T07:00:00Z"),
                Instant.parse("2026-05-16T19:00:00Z"),
                true
        );
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(verifiedPrincipal, null, List.of())
        );

        try {
            when(optimizationService.analyzeRouteWeatherAi(anyString(), any()))
                    .thenThrow(new IllegalArgumentException("Coordinates missing"));

            String jsonBody = "{}";
            mockMvc.perform(post("/api/v1/routes/weather-ai-analysis")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(post("/api/v1/routes/weather-ai-analysis")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

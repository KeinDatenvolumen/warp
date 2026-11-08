// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.api.controller;

import de.warp.bff.api.dto.ApiErrorResponse;
import de.warp.bff.api.dto.RouteWeatherAiAnalysisApiRequest;
import de.warp.bff.api.dto.RouteWeatherOptimizationApiRequest;
import de.warp.bff.application.service.RouteWeatherOptimizationService;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherAiAnalysisResponse;
import de.warp.bff.infrastructure.weather.WeatherApiDtos.RouteWeatherOptimizationResponse;
import de.warp.bff.security.AuthenticatedSessionPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteWeatherOptimizationController {

    private final RouteWeatherOptimizationService optimizationService;

    @Autowired
    public RouteWeatherOptimizationController(RouteWeatherOptimizationService optimizationService) {
        this.optimizationService = Objects.requireNonNull(optimizationService);
    }

    @PostMapping("/weather-optimization")
    public ResponseEntity<RouteWeatherOptimizationResponse> optimizeRouteWeather(
            @RequestBody(required = false) RouteWeatherOptimizationApiRequest request,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String userId = principal != null ? principal.userId() : "anonymous";
            RouteWeatherOptimizationResponse response = optimizationService.optimizeDeparture(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/weather-ai-analysis")
    public ResponseEntity<?> analyzeRouteWeatherAi(
            @RequestBody(required = false) RouteWeatherAiAnalysisApiRequest request,
            @AuthenticationPrincipal AuthenticatedSessionPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(new ApiErrorResponse(
                            URI.create("https://warp.local/problems/session-invalid"),
                            "Unauthorized",
                            HttpStatus.UNAUTHORIZED.value(),
                            "SESSION_INVALID",
                            "Für die KI-gestützte Wetteranalyse ist eine gültige Anmeldung erforderlich.",
                            "/api/v1/routes/weather-ai-analysis",
                            List.of()
                    ));
        }

        if (!principal.emailVerified()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(new ApiErrorResponse(
                            URI.create("https://warp.local/problems/email-not-verified"),
                            "Forbidden",
                            HttpStatus.FORBIDDEN.value(),
                            "EMAIL_NOT_VERIFIED",
                            "Die KI-gestützte Wetteranalyse steht ausschließlich für verifizierte Benutzerkonten zur Verfügung.",
                            "/api/v1/routes/weather-ai-analysis",
                            List.of()
                    ));
        }

        if (request == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            RouteWeatherAiAnalysisResponse response = optimizationService.analyzeRouteWeatherAi(principal.userId(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}

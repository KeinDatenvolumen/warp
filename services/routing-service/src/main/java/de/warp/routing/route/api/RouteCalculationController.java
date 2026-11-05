// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.warp.routing.route.application.RouteCalculationService;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/internal/v1/route-calculations")
public class RouteCalculationController {

    private static final String IDEMPOTENCY_HEADER =
            "Idempotency-Key";

    private static final String USER_ID_HEADER =
            "X-User-Id";

    private final RouteCalculationService routeCalculationService;
    private final ObjectMapper objectMapper;

    public RouteCalculationController(
            RouteCalculationService routeCalculationService,
            ObjectMapper objectMapper
    ) {
        this.routeCalculationService =
                routeCalculationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<RouteCalculationAcceptedResponse>
    startRouteCalculation(
            @RequestHeader(IDEMPOTENCY_HEADER)
            String idempotencyKey,
            @RequestBody
            StartRouteCalculationRequest request
    ) {
        RouteCalculationAcceptedResponse response =
                routeCalculationService.startCalculation(
                        idempotencyKey,
                        request
                );

        URI location = URI.create(
                "/internal/v1/route-calculations/"
                        + response.id()
        );

        return ResponseEntity.accepted()
                .location(location)
                .body(response);
    }

    @GetMapping("/{calculationId}")
    public RouteCalculationResponse getRouteCalculation(
            @PathVariable String calculationId,
            @RequestHeader(
                    value = USER_ID_HEADER,
                    required = false
            ) String requestedByUserId
    ) {
        RouteCalculationEntity calculation =
                requestedByUserId != null && !requestedByUserId.isBlank()
                        ? routeCalculationService.getCalculation(
                                calculationId,
                                requestedByUserId.trim()
                        )
                        : routeCalculationService.getCalculation(
                                calculationId
                        );

        return RouteCalculationResponse.from(
                calculation,
                objectMapper
        );
    }

    public RouteCalculationResponse getRouteCalculation(
            String calculationId
    ) {
        return getRouteCalculation(calculationId, null);
    }
}
package de.warp.routing.route.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import de.warp.routing.route.application.RouteCalculationNotFoundException;
import de.warp.routing.route.application.RouteCalculationService;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RouteCalculationExceptionHandlerTest {

    @Mock
    private RouteCalculationService routeCalculationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RouteCalculationController controller =
                new RouteCalculationController(
                        routeCalculationService,
                        new ObjectMapper()
                );

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(
                        new RouteCalculationExceptionHandler()
                )
                .build();
    }

    @Test
    void returnsInvalidRouteRequestForMissingIdempotencyKey()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/internal/v1/route-calculations"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(validRequestBody())
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.type")
                                .value(
                                        "https://warp.local/problems/"
                                                + "invalid-route-request"
                                )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "INVALID_ROUTE_REQUEST"
                                )
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Die Routenanfrage ist ungültig."
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value(
                                        "/internal/v1/"
                                                + "route-calculations"
                                )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .isArray()
                );
    }

    @Test
    void returnsInvalidRouteRequestForMalformedJson()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/internal/v1/route-calculations"
                        )
                                .header(
                                        "Idempotency-Key",
                                        "idempotency-key-123"
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("{invalid-json")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "INVALID_ROUTE_REQUEST"
                                )
                );
    }

    @Test
    void returnsNotFoundForUnknownCalculation()
            throws Exception {
        when(
                routeCalculationService.getCalculation(
                        "calc_missing"
                )
        ).thenThrow(
                new RouteCalculationNotFoundException(
                        "calc_missing"
                )
        );

        mockMvc.perform(
                        get(
                                "/internal/v1/"
                                        + "route-calculations/"
                                        + "calc_missing"
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.type")
                                .value(
                                        "https://warp.local/problems/"
                                                + "route-calculation-"
                                                + "not-found"
                                )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Not Found")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "ROUTE_CALCULATION_NOT_FOUND"
                                )
                )
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "Die Routenberechnung wurde "
                                                + "nicht gefunden."
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value(
                                        "/internal/v1/"
                                                + "route-calculations/"
                                                + "calc_missing"
                                )
                );
    }

    @Test
    void forwardsExistingCorrelationId()
            throws Exception {
        when(
                routeCalculationService.getCalculation(
                        "calc_missing"
                )
        ).thenThrow(
                new RouteCalculationNotFoundException(
                        "calc_missing"
                )
        );

        mockMvc.perform(
                        get(
                                "/internal/v1/"
                                        + "route-calculations/"
                                        + "calc_missing"
                        )
                                .header(
                                        "X-Correlation-Id",
                                        "correlation-123"
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(
                        header().string(
                                "X-Correlation-Id",
                                "correlation-123"
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value("correlation-123")
                );
    }

    private String validRequestBody() {
        return """
                {
                  "requestedByUserId": "usr_123",
                  "start": {
                    "label": "Mosbach",
                    "latitude": 49.3536,
                    "longitude": 9.1511
                  },
                  "destination": {
                    "label": "Bad Mergentheim",
                    "latitude": 49.4924,
                    "longitude": 9.7736
                  },
                  "departureTime": "2026-10-22T06:00:00Z",
                  "activityProfile": {
                    "code": "BICYCLE",
                    "routingProfile": "cycling-regular"
                  }
                }
                """;
    }
}
package de.warp.bff.infrastructure.routing;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import de.warp.bff.application.exception
        .InvalidRouteRequestException;
import de.warp.bff.application.exception
        .RouteCalculationNotFoundException;
import de.warp.bff.application.exception
        .RoutingServiceUnavailableException;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationAcceptedResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalRouteCalculationResponse;
import de.warp.bff.infrastructure.routing.dto
        .InternalStartRouteCalculationRequest;
import de.warp.bff.infrastructure.routing.dto
        .RoutingProblemResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
public class RoutingClient {

    private static final String IDEMPOTENCY_HEADER =
            "Idempotency-Key";

    private static final String USER_ID_HEADER =
            "X-User-Id";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public RoutingClient(
            @Qualifier("routingServiceRestClient")
            RestClient restClient,
            JsonMapper jsonMapper
    ) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    public InternalRouteCalculationAcceptedResponse
    startRouteCalculation(
            String idempotencyKey,
            InternalStartRouteCalculationRequest request
    ) {
        try {
            InternalRouteCalculationAcceptedResponse response =
                    restClient.post()
                            .uri(
                                    "/internal/v1/"
                                            + "route-calculations"
                            )
                            .header(
                                    IDEMPOTENCY_HEADER,
                                    idempotencyKey
                            )
                            .body(request)
                            .retrieve()
                            .body(
                                    InternalRouteCalculationAcceptedResponse
                                            .class
                            );

            if (response == null) {
                throw new RoutingServiceUnavailableException();
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw translateError(exception);
        } catch (RestClientException exception) {
            throw new RoutingServiceUnavailableException(
                    exception
            );
        }
    }

    public InternalRouteCalculationResponse
    getRouteCalculation(
            String userId,
            String calculationId
    ) {
        try {
            var request =
                    restClient.get()
                            .uri(
                                    "/internal/v1/"
                                            + "route-calculations/"
                                            + "{calculationId}",
                                    calculationId
                            );

            if (userId != null && !userId.isBlank()) {
                request.header(USER_ID_HEADER, userId.trim());
            }

            InternalRouteCalculationResponse response =
                    request.retrieve()
                            .body(
                                    InternalRouteCalculationResponse
                                            .class
                            );

            if (response == null) {
                throw new RoutingServiceUnavailableException();
            }

            return response;
        } catch (RestClientResponseException exception) {
            throw translateError(exception);
        } catch (RestClientException exception) {
            throw new RoutingServiceUnavailableException(
                    exception
            );
        }
    }

    public InternalRouteCalculationResponse
    getRouteCalculation(
            String calculationId
    ) {
        return getRouteCalculation(null, calculationId);
    }

    private RuntimeException translateError(
            RestClientResponseException exception
    ) {
        RoutingProblemResponse problem =
                readProblem(exception);

        if (exception.getStatusCode()
                .isSameCodeAs(HttpStatus.BAD_REQUEST)
                && "INVALID_ROUTE_REQUEST".equals(
                problem.code()
        )) {
            return new InvalidRouteRequestException();
        }

        if (exception.getStatusCode()
                .isSameCodeAs(HttpStatus.NOT_FOUND)
                && "ROUTE_CALCULATION_NOT_FOUND".equals(
                problem.code()
        )) {
            return new RouteCalculationNotFoundException();
        }

        return new RoutingServiceUnavailableException(
                exception
        );
    }

    private RoutingProblemResponse readProblem(
            RestClientResponseException exception
    ) {
        try {
            RoutingProblemResponse response =
                    jsonMapper.readValue(
                            exception.getResponseBodyAsByteArray(),
                            RoutingProblemResponse.class
                    );

            if (response != null) {
                return response;
            }
        } catch (RuntimeException ignored) {
            // Eine unlesbare Downstream-Antwort wird
            // nicht ungeprüft weitergegeben.
        }

        return new RoutingProblemResponse(
                null,
                null,
                exception.getStatusCode().value(),
                null,
                null,
                null,
                null,
                null
        );
    }
}
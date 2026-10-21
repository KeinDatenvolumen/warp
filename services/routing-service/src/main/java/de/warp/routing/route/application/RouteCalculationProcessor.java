package de.warp.routing.route.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;

import de.warp.routing.infrastructure.routing.OpenRouteServiceClient;
import de.warp.routing.infrastructure.routing.OpenRouteServiceResponse;
import de.warp.routing.infrastructure.routing.RouteNotFoundException;
import de.warp.routing.infrastructure.routing.RoutingServiceUnavailableException;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import de.warp.routing.route.persistence.RouteCalculationRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class RouteCalculationProcessor {

    private static final String ROUTE_NOT_FOUND =
            "ROUTE_NOT_FOUND";

    private static final String ROUTING_SERVICE_UNAVAILABLE =
            "ROUTING_SERVICE_UNAVAILABLE";

    private final RouteCalculationRepository repository;
    private final OpenRouteServiceClient openRouteServiceClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RouteCalculationProcessor(
            RouteCalculationRepository repository,
            OpenRouteServiceClient openRouteServiceClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = repository;
        this.openRouteServiceClient = openRouteServiceClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void process(String calculationId) {
        RouteCalculationEntity calculation =
                repository.findById(calculationId)
                        .orElseThrow(
                                () -> new RouteCalculationNotFoundException(
                                        calculationId
                                )
                        );

        calculation.markProcessing(clock.instant());
        repository.saveAndFlush(calculation);

        try {
            OpenRouteServiceResponse.Feature route =
                    openRouteServiceClient.calculateRoute(
                            calculation.getRoutingProfile(),
                            calculation.getStartLatitude(),
                            calculation.getStartLongitude(),
                            calculation.getDestinationLatitude(),
                            calculation.getDestinationLongitude()
                    );

            completeCalculation(calculation, route);
        } catch (RouteNotFoundException exception) {
            failCalculation(
                    calculation,
                    ROUTE_NOT_FOUND,
                    "Für Start, Ziel und Aktivitätsprofil "
                            + "konnte keine Route gefunden werden."
            );
        } catch (RoutingServiceUnavailableException exception) {
            failCalculation(
                    calculation,
                    ROUTING_SERVICE_UNAVAILABLE,
                    "Der externe Routing-Dienst ist "
                            + "vorübergehend nicht verfügbar."
            );
        }
    }

    private void completeCalculation(
            RouteCalculationEntity calculation,
            OpenRouteServiceResponse.Feature route
    ) {
        OpenRouteServiceResponse.Summary summary =
                route.properties().summary();

        double distanceMeters = summary.distance();
        double durationSeconds = summary.duration();

        validateRouteResult(
                distanceMeters,
                durationSeconds
        );

        long roundedDurationSeconds =
                Math.round(durationSeconds);

        Instant arrivalTime =
                calculation.getDepartureTime()
                        .plusSeconds(roundedDurationSeconds);

        String geometry = objectMapper.writeValueAsString(
                route.geometry()
        );

        String segments = objectMapper.writeValueAsString(
                route.properties().segments()
        );

        calculation.complete(
                arrivalTime,
                distanceMeters,
                durationSeconds,
                geometry,
                segments,
                clock.instant()
        );

        repository.saveAndFlush(calculation);
    }

    private void failCalculation(
            RouteCalculationEntity calculation,
            String errorCode,
            String errorMessage
    ) {
        calculation.fail(
                errorCode,
                errorMessage,
                clock.instant()
        );

        repository.saveAndFlush(calculation);
    }

    private void validateRouteResult(
            double distanceMeters,
            double durationSeconds
    ) {
        if (!Double.isFinite(distanceMeters)
                || distanceMeters < 0
                || !Double.isFinite(durationSeconds)
                || durationSeconds < 0) {
            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst hat ungültige "
                            + "Routendaten geliefert."
            );
        }
    }
}
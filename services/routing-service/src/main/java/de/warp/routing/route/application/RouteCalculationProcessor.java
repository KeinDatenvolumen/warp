// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
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
        java.util.List<OpenRouteServiceResponse.Segment> segments =
                route.properties().segments();

        validateRouteResult(
                distanceMeters,
                durationSeconds,
                segments
        );

        long roundedDurationSeconds =
                Math.round(durationSeconds);

        Instant arrivalTime =
                calculation.getDepartureTime()
                        .plusSeconds(roundedDurationSeconds);

        String geometry = objectMapper.writeValueAsString(
                route.geometry()
        );

        String segmentsJson = objectMapper.writeValueAsString(
                segments
        );

        calculation.complete(
                arrivalTime,
                distanceMeters,
                durationSeconds,
                geometry,
                segmentsJson,
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
            double durationSeconds,
            java.util.List<OpenRouteServiceResponse.Segment> segments
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

        if (segments == null || segments.isEmpty()) {
            throw new RoutingServiceUnavailableException(
                    "Der externe Routing-Dienst hat keine "
                            + "Routensegmente geliefert."
            );
        }

        double totalSegmentDistance = 0.0;
        double totalSegmentDuration = 0.0;

        for (OpenRouteServiceResponse.Segment segment : segments) {
            if (segment == null
                    || !Double.isFinite(segment.distance())
                    || segment.distance() < 0
                    || !Double.isFinite(segment.duration())
                    || segment.duration() < 0) {
                throw new RoutingServiceUnavailableException(
                        "Der externe Routing-Dienst hat ungültige "
                                + "Segmentdaten geliefert."
                );
            }

            totalSegmentDistance += segment.distance();
            totalSegmentDuration += segment.duration();
        }

        double distanceDiff = Math.abs(totalSegmentDistance - distanceMeters);
        double durationDiff = Math.abs(totalSegmentDuration - durationSeconds);

        if (distanceDiff > Math.max(5.0, distanceMeters * 0.01)
                || durationDiff > Math.max(5.0, durationSeconds * 0.01)) {
            throw new RoutingServiceUnavailableException(
                    "Die Summe der Segmentwerte weicht unplausibel "
                            + "von den Gesamtwerten der Route ab."
            );
        }
    }
}
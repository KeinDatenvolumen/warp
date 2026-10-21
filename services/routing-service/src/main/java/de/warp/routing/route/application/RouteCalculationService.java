package de.warp.routing.route.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.warp.routing.route.api.RouteCalculationAcceptedResponse;
import de.warp.routing.route.api.StartRouteCalculationRequest;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import de.warp.routing.route.persistence.RouteCalculationRepository;

@Service
public class RouteCalculationService {

    private final RouteCalculationRepository repository;
    private final Clock clock;

    public RouteCalculationService(
            RouteCalculationRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public RouteCalculationAcceptedResponse startCalculation(
            StartRouteCalculationRequest request
    ) {
        validateRequest(request);

        Instant createdAt = clock.instant();

        RouteCalculationEntity calculation =
                new RouteCalculationEntity(
                        generateCalculationId(),
                        request.requestedByUserId().trim(),
                        request.start().label().trim(),
                        request.start().latitude(),
                        request.start().longitude(),
                        request.destination().label().trim(),
                        request.destination().latitude(),
                        request.destination().longitude(),
                        request.departureTime(),
                        request.activityProfile().code(),
                        request.activityProfile()
                                .routingProfile()
                                .trim(),
                        createdAt
                );

        RouteCalculationEntity savedCalculation =
                repository.save(calculation);

        return new RouteCalculationAcceptedResponse(
                savedCalculation.getId(),
                savedCalculation.getStatus(),
                savedCalculation.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public RouteCalculationEntity getCalculation(
            String calculationId
    ) {
        if (calculationId == null
                || calculationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Die Berechnungs-ID darf nicht leer sein."
            );
        }

        return repository.findById(calculationId)
                .orElseThrow(
                        () -> new RouteCalculationNotFoundException(
                                calculationId
                        )
                );
    }

    private String generateCalculationId() {
        return "calc_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private void validateRequest(
            StartRouteCalculationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Die Routenanfrage darf nicht fehlen."
            );
        }

        if (request.requestedByUserId() == null
                || request.requestedByUserId().isBlank()) {
            throw new IllegalArgumentException(
                    "Die Benutzer-ID darf nicht leer sein."
            );
        }

        if (request.start() == null) {
            throw new IllegalArgumentException(
                    "Der Startpunkt darf nicht fehlen."
            );
        }

        validatePoint(
                request.start().label(),
                request.start().latitude(),
                request.start().longitude(),
                "Startpunkt"
        );

        if (request.destination() == null) {
            throw new IllegalArgumentException(
                    "Der Zielpunkt darf nicht fehlen."
            );
        }

        validatePoint(
                request.destination().label(),
                request.destination().latitude(),
                request.destination().longitude(),
                "Zielpunkt"
        );

        if (request.departureTime() == null) {
            throw new IllegalArgumentException(
                    "Die Abfahrtszeit darf nicht fehlen."
            );
        }

        if (request.activityProfile() == null) {
            throw new IllegalArgumentException(
                    "Das Aktivitätsprofil darf nicht fehlen."
            );
        }

        if (request.activityProfile().code() == null) {
            throw new IllegalArgumentException(
                    "Der Aktivitätsprofil-Code darf nicht fehlen."
            );
        }

        if (request.activityProfile().routingProfile() == null
                || request.activityProfile()
                .routingProfile()
                .isBlank()) {
            throw new IllegalArgumentException(
                    "Das Routingprofil darf nicht leer sein."
            );
        }
    }

    private void validatePoint(
            String label,
            double latitude,
            double longitude,
            String fieldName
    ) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + ": Die Bezeichnung darf nicht leer sein."
            );
        }

        if (!Double.isFinite(latitude)
                || latitude < -90
                || latitude > 90) {
            throw new IllegalArgumentException(
                    fieldName
                            + ": Der Breitengrad muss zwischen "
                            + "-90 und 90 liegen."
            );
        }

        if (!Double.isFinite(longitude)
                || longitude < -180
                || longitude > 180) {
            throw new IllegalArgumentException(
                    fieldName
                            + ": Der Längengrad muss zwischen "
                            + "-180 und 180 liegen."
            );
        }
    }
}
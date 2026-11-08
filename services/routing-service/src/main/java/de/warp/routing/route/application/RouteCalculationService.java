// [KI-Log Nr. KI-157] (ac90611) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement route calculation workflow
// [KI-Log Nr. KI-158] (e710e2c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 support idempotent route calculation requests
// [KI-Log Nr. KI-159] (906871f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-173] (fd1cf9e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement route calculation workflow
// [KI-Log Nr. KI-174] (0d8cf61) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 support idempotent route calculation requests
// [KI-Log Nr. KI-175] (b18893e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose internal route calculation API
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.application;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import de.warp.routing.route.api.RouteCalculationAcceptedResponse;
import de.warp.routing.route.api.StartRouteCalculationRequest;
import de.warp.routing.route.persistence.RouteCalculationEntity;
import de.warp.routing.route.persistence.RouteCalculationRepository;

// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity Gemini 3.8 Flash High) für idempotente Routenberechnung
@Service
public class RouteCalculationService {

    private final RouteCalculationRepository repository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public RouteCalculationService(
            RouteCalculationRepository repository,
            Clock clock,
            ApplicationEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }
    @Transactional
    public RouteCalculationAcceptedResponse startCalculation(
            String idempotencyKey,
            StartRouteCalculationRequest request
    ) {
        validateIdempotencyKey(idempotencyKey);
        validateRequest(request);

        String normalizedIdempotencyKey =
                idempotencyKey.trim();

        RouteCalculationEntity existingCalculation =
                repository.findByIdempotencyKey(
                        normalizedIdempotencyKey
                ).orElse(null);

        if (existingCalculation != null) {
            return toAcceptedResponse(existingCalculation);
        }

        Instant createdAt = clock.instant();

        RouteCalculationEntity calculation =
                new RouteCalculationEntity(
                        generateCalculationId(),
                        request.requestedByUserId().trim(),
                        normalizedIdempotencyKey,
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

        eventPublisher.publishEvent(
                new RouteCalculationCreatedEvent(
                        savedCalculation.getId()
                )
        );

        return toAcceptedResponse(savedCalculation);
    }

    @Transactional(readOnly = true)
    public RouteCalculationEntity getCalculation(
            String calculationId,
            String requestedByUserId
    ) {
        if (calculationId == null
                || calculationId.isBlank()) {
            throw new IllegalArgumentException(
                    "Die Berechnungs-ID darf nicht leer sein."
            );
        }

        if (requestedByUserId != null && !requestedByUserId.isBlank()) {
            return repository.findByIdAndRequestedByUserId(
                    calculationId,
                    requestedByUserId.trim()
            ).orElseThrow(
                    () -> new RouteCalculationNotFoundException(
                            calculationId
                    )
            );
        }

        return repository.findById(calculationId)
                .orElseThrow(
                        () -> new RouteCalculationNotFoundException(
                                calculationId
                        )
                );
    }

    @Transactional(readOnly = true)
    public RouteCalculationEntity getCalculation(
            String calculationId
    ) {
        return getCalculation(calculationId, null);
    }

    private String generateCalculationId() {
        return "calc_" + UUID.randomUUID()
                .toString()
                .replace("-", "");
    }

    private void validateIdempotencyKey(
            String idempotencyKey
    ) {
        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Der Idempotency-Key darf nicht leer sein."
            );
        }

        if (idempotencyKey.trim().length() > 128) {
            throw new IllegalArgumentException(
                    "Der Idempotency-Key darf höchstens "
                            + "128 Zeichen enthalten."
            );
        }
    }

    private RouteCalculationAcceptedResponse toAcceptedResponse(
            RouteCalculationEntity calculation
    ) {
        return new RouteCalculationAcceptedResponse(
                calculation.getId(),
                calculation.getStatus(),
                calculation.getCreatedAt()
        );
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
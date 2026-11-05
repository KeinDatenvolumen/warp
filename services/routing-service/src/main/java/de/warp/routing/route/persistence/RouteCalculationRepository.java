// [KI-Log Nr. KI-12] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Asynchrone Routenberechnung via OpenRouteService (WARP-012)
package de.warp.routing.route.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteCalculationRepository
        extends JpaRepository<RouteCalculationEntity, String> {

    Optional<RouteCalculationEntity> findByIdempotencyKey(
            String idempotencyKey
    );

    Optional<RouteCalculationEntity> findByIdAndRequestedByUserId(
            String id,
            String requestedByUserId
    );

    List<RouteCalculationEntity> findByRequestedByUserIdOrderByCreatedAtDesc(
            String requestedByUserId
    );
}
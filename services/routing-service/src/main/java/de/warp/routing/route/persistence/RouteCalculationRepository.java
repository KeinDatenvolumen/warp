// [KI-Log Nr. KI-156] (7fb0636) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 persist route calculations
// [KI-Log Nr. KI-158] (e710e2c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 support idempotent route calculation requests
// [KI-Log Nr. KI-172] (45536d2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 persist route calculations
// [KI-Log Nr. KI-174] (0d8cf61) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 support idempotent route calculation requests
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
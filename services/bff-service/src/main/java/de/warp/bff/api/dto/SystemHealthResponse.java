// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
package de.warp.bff.api.dto;

import java.time.Instant;
import java.util.List;

public record SystemHealthResponse(
        String status,
        Instant checkedAt,
        int healthyServices,
        int totalServices,
        List<ServiceHealthResponse> services
) {
}
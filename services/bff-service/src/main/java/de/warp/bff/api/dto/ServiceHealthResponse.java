// [KI-Log Nr. KI-02] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Monorepo-Setup, Gradle & Docker (WARP-001)
package de.warp.bff.api.dto;

public record ServiceHealthResponse(
        String service,
        String status,
        boolean reachable,
        long responseTimeMs,
        String checkedUrl
) {
}
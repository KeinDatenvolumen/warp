// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.graphql;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import de.warp.bff.api.dto.ServiceHealthResponse;
import de.warp.bff.api.dto.SystemHealthResponse;
import de.warp.bff.application.service.SystemHealthService;

@Controller
public class SystemHealthGraphQlController {

    private final SystemHealthService systemHealthService;
    private final String forcedError;

    public SystemHealthGraphQlController(
            SystemHealthService systemHealthService,
            @Value("${warp.graphql.test-error:}") String forcedError
    ) {
        this.systemHealthService = systemHealthService;
        this.forcedError = forcedError;
    }

    @QueryMapping
    public SystemHealthPayload systemHealth() {
        if ("bad-request".equals(forcedError)) {
            throw new IllegalArgumentException(
                    "Diese interne BAD_REQUEST-Meldung darf nicht erscheinen."
            );
        }

        if ("internal".equals(forcedError)) {
            throw new IllegalStateException(
                    "Diese interne Fehlermeldung darf nicht erscheinen."
            );
        }

        SystemHealthResponse response =
                systemHealthService.getSystemHealth();

        List<ServiceHealthPayload> services = response.services()
                .stream()
                .map(this::toServiceHealthPayload)
                .toList();

        return new SystemHealthPayload(
                SystemHealthStatus.valueOf(response.status()),
                response.checkedAt().atOffset(ZoneOffset.UTC),
                response.healthyServices(),
                response.totalServices(),
                services
        );
    }

    private ServiceHealthPayload toServiceHealthPayload(
            ServiceHealthResponse response
    ) {
        return new ServiceHealthPayload(
                response.service(),
                ServiceHealthStatus.valueOf(response.status()),
                response.reachable(),
                Math.toIntExact(response.responseTimeMs())
        );
    }

    public record SystemHealthPayload(
            SystemHealthStatus status,
            OffsetDateTime checkedAt,
            int healthyServices,
            int totalServices,
            List<ServiceHealthPayload> services
    ) {
    }

    public record ServiceHealthPayload(
            String service,
            ServiceHealthStatus status,
            boolean reachable,
            int responseTimeMs
    ) {
    }

    public enum SystemHealthStatus {
        UP,
        DEGRADED
    }

    public enum ServiceHealthStatus {
        UP,
        DOWN,
        OUT_OF_SERVICE,
        UNKNOWN,
        UNAVAILABLE
    }
}
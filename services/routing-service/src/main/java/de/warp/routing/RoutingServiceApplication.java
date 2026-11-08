// [KI-Log Nr. KI-002] (8c3033f) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize WaRP monorepo and BFF service (WARP-001)
// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-236] (201cf90) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-035): add in-memory caching
// [KI-Log Nr. KI-237] (d313c4e) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-035 improve code, implement best practises
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RoutingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RoutingServiceApplication.class, args);
    }
}
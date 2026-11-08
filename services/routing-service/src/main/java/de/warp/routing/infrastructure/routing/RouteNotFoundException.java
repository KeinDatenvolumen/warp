// [KI-Log Nr. KI-154] (2dcf6bf) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-170] (3b5d8a2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement OpenRouteService client
// [KI-Log Nr. KI-182] (0927937) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: some minor changes to security, rebasing (WARP-033)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.infrastructure.routing;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(String message) {
        super(message);
    }

    public RouteNotFoundException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
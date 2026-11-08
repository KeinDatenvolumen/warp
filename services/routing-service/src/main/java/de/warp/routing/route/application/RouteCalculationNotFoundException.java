// [KI-Log Nr. KI-157] (ac90611) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement route calculation workflow
// [KI-Log Nr. KI-173] (fd1cf9e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 implement route calculation workflow
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.route.application;

public class RouteCalculationNotFoundException
        extends RuntimeException {

    public RouteCalculationNotFoundException(
            String calculationId
    ) {
        super(
                "Die Routenberechnung wurde nicht gefunden: "
                        + calculationId
        );
    }
}
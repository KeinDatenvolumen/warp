// [KI-Log Nr. KI-062] (1b34cbd) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add central activity profile configuration (WARP-033)
// [KI-Log Nr. KI-161] (078f95b) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose routing engine profile in activity profiles
// [KI-Log Nr. KI-177] (c0720fe) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 expose routing engine profile in activity profiles
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.routing.profile.domain;

public enum RoutingProfileCode {

    BICYCLE("cycling-regular"),
    MOTORCYCLE("driving-car"),
    FOOT("foot-hiking");

    private final String externalCode;

    RoutingProfileCode(String externalCode) {
        this.externalCode = externalCode;
    }

    public String externalCode() {
        return externalCode;
    }
}
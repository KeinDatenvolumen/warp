// [KI-Log Nr. KI-18] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Profilseite & Theme-System (WARP-033)
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
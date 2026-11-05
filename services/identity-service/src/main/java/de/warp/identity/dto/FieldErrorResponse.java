// [KI-Log Nr. KI-07] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Benutzerregistrierung & Validierung (WARP-005)
package de.warp.identity.dto;

public record FieldErrorResponse(
        String field,
        String code,
        String message
) {
}
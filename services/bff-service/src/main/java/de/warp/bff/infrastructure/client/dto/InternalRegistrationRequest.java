// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.infrastructure.client.dto;

public record InternalRegistrationRequest(
        String name,
        String email,
        String password
) {
}
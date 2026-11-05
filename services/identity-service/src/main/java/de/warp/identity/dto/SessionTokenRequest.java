// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionTokenRequest(
        @NotBlank
        String sessionToken
) {
}
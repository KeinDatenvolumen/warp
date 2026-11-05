// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

public record CreateSessionRequest(

        @NotBlank(message = "Die E-Mail-Adresse muss angegeben werden.")
        @Email(message = "Die E-Mail-Adresse ist ungültig.")
        String email,

        @NotNull(message = "Das Passwort muss angegeben werden.")
        String password
) {

    public CreateSessionRequest {
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
    }
}
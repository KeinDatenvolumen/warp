// [KI-Log Nr. KI-051] (cfd2cd9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/warp-005: implement user registration with Tests
// [KI-Log Nr. KI-068] (a7d6e5a) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat/WARP-005: implement user registration
// [KI-Log Nr. KI-192] (6763409) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-008): implement password reset
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.dto;

import de.warp.identity.security.PasswordRequirements;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record RegistrationRequest(

        @NotBlank(message = "Der Name darf nicht leer sein.")
        String name,

        @NotBlank(message = "Die E-Mail-Adresse muss angegeben werden.")
        @Email(message = "Die E-Mail-Adresse ist ungültig.")
        String email,

        @NotNull(message = "Das Passwort muss angegeben werden.")
        @Size(
                min = PasswordRequirements.MIN_LENGTH,
                message = "Das Passwort muss mindestens zwölf Zeichen lang sein."
        )
        String password
) {
        public RegistrationRequest {
                if (email != null) {
                        email = email.trim().toLowerCase(Locale.ROOT);
                }
        }
}
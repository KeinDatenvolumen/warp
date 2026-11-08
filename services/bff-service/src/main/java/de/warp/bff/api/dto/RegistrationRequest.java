// [KI-Log Nr. KI-079] (44a3ff4) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: added registration request and response types for bff (WARP-005)
// [KI-Log Nr. KI-106] (0f8e35c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: added registration request and response types for bff (WARP-005)
// [KI-Log Nr. KI-117] (76887ed) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: improve overall handling of registration (WARP-005)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "Bitte gib deinen Namen ein.")
        String name,

        @NotBlank(message = "Bitte gib deine E-Mail-Adresse ein.")
        @Email(message = "Bitte gib eine gültige E-Mail-Adresse ein.")
        String email,

        @NotBlank(message = "Bitte gib ein Passwort ein.")
        @Size(
                min = 12,
                message = "Das Passwort muss mindestens zwölf Zeichen lang sein."
        )
        String password
) {
}
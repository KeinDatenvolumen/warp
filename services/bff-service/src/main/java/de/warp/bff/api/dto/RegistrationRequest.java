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
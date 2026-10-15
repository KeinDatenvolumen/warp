package de.warp.identity.dto;

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
                min = 12,
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
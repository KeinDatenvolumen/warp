package de.warp.identity.controller;

import de.warp.identity.dto.RegistrationRequest;
import de.warp.identity.dto.RegistrationResponse;
import de.warp.identity.service.RegistrationService;
import de.warp.identity.exceptions.EmailAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.never;

@WebMvcTest(RegistrationController.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void shouldReturnCreatedForValidRegistration() throws Exception {
        RegistrationResponse response = new RegistrationResponse(
                "usr_123",
                "Max Mustermann",
                "max@example.de",
                false,
                Instant.parse("2026-10-03T10:30:00Z"),
                "Das Benutzerkonto wurde erstellt. Eine Bestätigungs-E-Mail wurde versendet."
        );

        when(registrationService.register(any(RegistrationRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                  "name": "Max Mustermann",
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.id").value("usr_123"))
                .andExpect(jsonPath("$.name").value("Max Mustermann"))
                .andExpect(jsonPath("$.email").value("max@example.de"))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-03T10:30:00Z"))
                .andExpect(jsonPath("$.message").value(
                        "Das Benutzerkonto wurde erstellt. Eine Bestätigungs-E-Mail wurde versendet."
                ))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        verify(registrationService)
                .register(any(RegistrationRequest.class));
    }

    @Test
    void shouldReturnUnprocessableEntityForShortPassword() throws Exception {
        String requestBody = """
            {
              "name": "Max Mustermann",
              "email": "max@example.de",
              "password": "zu-kurz"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REGISTRATION_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Die Registrierungsdaten sind ungültig."))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].code")
                        .value("INVALID_PASSWORD"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value("Das Passwort muss mindestens zwölf Zeichen lang sein."));

        verify(registrationService, never())
                .register(any(RegistrationRequest.class));
    }

    @Test
    void shouldReturnUnprocessableEntityForMissingName() throws Exception {
        String requestBody = """
            {
              "email": "max@example.de",
              "password": "MeinSicheresPasswort123"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REGISTRATION_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Die Registrierungsdaten sind ungültig."))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].code")
                        .value("INVALID_NAME"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value("Der Name darf nicht leer sein."));

        verify(registrationService, never())
                .register(any(RegistrationRequest.class));
    }

    @Test
    void shouldReturnUnprocessableEntityForInvalidEmail() throws Exception {
        String requestBody = """
            {
              "name": "Max Mustermann",
              "email": "keine-gueltige-email",
              "password": "MeinSicheresPasswort123"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REGISTRATION_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("Die Registrierungsdaten sind ungültig."))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("email"))
                .andExpect(jsonPath("$.fieldErrors[0].code")
                        .value("INVALID_EMAIL"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value("Die E-Mail-Adresse ist ungültig."));

        verify(registrationService, never())
                .register(any(RegistrationRequest.class));
    }

    @Test
    void shouldReturnConflictForAlreadyRegisteredEmail() throws Exception {
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new EmailAlreadyExistsException());

        String requestBody = """
            {
              "name": "Max Mustermann",
              "email": "max@example.de",
              "password": "MeinSicheresPasswort123"
            }
            """;

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message")
                        .value("Für diese E-Mail-Adresse existiert bereits ein Benutzerkonto."))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        verify(registrationService)
                .register(any(RegistrationRequest.class));
    }
}
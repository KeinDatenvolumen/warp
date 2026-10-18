package de.warp.bff.api.controller;

import de.warp.bff.api.dto.RegistrationResponse;
import de.warp.bff.application.exception.RegistrationRejectedException;
import de.warp.bff.application.service.RegistrationService;
import de.warp.bff.application.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void shouldRegisterUser() throws Exception {
        when(registrationService.register(any()))
                .thenReturn(
                        new RegistrationResponse(
                                "usr_123",
                                "Max Mustermann",
                                "max@example.de",
                                false,
                                Instant.parse(
                                        "2026-10-18T10:00:00Z"
                                ),
                                "Das Benutzerkonto wurde "
                                        + "erfolgreich erstellt."
                        )
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Max Mustermann",
                                          "email": "max@example.de",
                                          "password": "MeinSicheresPasswort123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value("usr_123")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Das Benutzerkonto wurde "
                                                + "erfolgreich erstellt."
                                )
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                );

        verify(registrationService)
                .register(any());
    }

    @Test
    void shouldRejectInvalidRegistrationRequest()
            throws Exception {
        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "",
                                          "email": "ungueltig",
                                          "password": "kurz"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isUnprocessableContent()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "INVALID_REGISTRATION_REQUEST"
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/api/v1/users")
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .isArray()
                );
    }

    @Test
    void shouldReturnConflictForExistingEmail()
            throws Exception {
        when(registrationService.register(any()))
                .thenThrow(
                        new RegistrationRejectedException(
                                org.springframework.http.HttpStatus
                                        .CONFLICT,
                                "EMAIL_ALREADY_EXISTS",
                                "Für diese E-Mail-Adresse ist "
                                        + "bereits ein Benutzerkonto "
                                        + "vorhanden.",
                                List.of()
                        )
                );

        mockMvc.perform(
                        post("/api/v1/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "name": "Max Mustermann",
                                          "email": "max@example.de",
                                          "password": "MeinSicheresPasswort123"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "EMAIL_ALREADY_EXISTS"
                                )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/api/v1/users")
                );
    }
}
package de.warp.identity.controller;

import de.warp.identity.dto.RegistrationRequest;
import de.warp.identity.dto.RegistrationResponse;
import de.warp.identity.exceptions.EmailAlreadyExistsException;
import de.warp.identity.filter.CorrelationIdFilter;
import de.warp.identity.filter.InternalServiceAuthenticationFilter;
import de.warp.identity.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = RegistrationController.class,
        properties = {
                "warp.security.internal-service-token="
                        + "test-internal-service-token"
        }
)
class RegistrationControllerTest {

    private static final String CORRELATION_ID =
            "01J9ABCDEF1234567890";

    private static final String INTERNAL_SERVICE_TOKEN =
            "test-internal-service-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void shouldReturnCreatedForValidRegistration()
            throws Exception {
        RegistrationResponse response =
                new RegistrationResponse(
                        "usr_123",
                        "Max Mustermann",
                        "max@example.de",
                        false,
                        Instant.parse(
                                "2026-10-03T10:30:00Z"
                        )
                );

        when(registrationService.register(
                any(RegistrationRequest.class)
        )).thenReturn(response);

        String requestBody = """
                {
                  "name": "Max Mustermann",
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value("usr_123")
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Max Mustermann")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("max@example.de")
                )
                .andExpect(
                        jsonPath("$.emailVerified")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.createdAt")
                                .value("2026-10-03T10:30:00Z")
                )
                .andExpect(
                        jsonPath("$.message")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );

        verify(registrationService)
                .register(
                        any(RegistrationRequest.class)
                );
    }

    @Test
    void shouldReturnUnprocessableContentForShortPassword()
            throws Exception {
        String requestBody = """
                {
                  "name": "Max Mustermann",
                  "email": "max@example.de",
                  "password": "zu-kurz"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
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
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.type").value(
                                "https://warp.local/problems/"
                                        + "invalid-registration-request"
                        )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Unprocessable Content")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(422)
                )
                .andExpect(
                        jsonPath("$.code").value(
                                "INVALID_REGISTRATION_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Die Registrierungsdaten sind ungültig."
                        )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/internal/v1/users")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].field")
                                .value("password")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].code")
                                .value("INVALID_PASSWORD")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].message").value(
                                "Das Passwort muss mindestens "
                                        + "zwölf Zeichen lang sein."
                        )
                );

        verify(registrationService, never())
                .register(
                        any(RegistrationRequest.class)
                );
    }

    @Test
    void shouldReturnUnprocessableContentForMissingName()
            throws Exception {
        String requestBody = """
                {
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
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
                        jsonPath("$.status")
                                .value(422)
                )
                .andExpect(
                        jsonPath("$.code").value(
                                "INVALID_REGISTRATION_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Die Registrierungsdaten sind ungültig."
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].field")
                                .value("name")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].code")
                                .value("INVALID_NAME")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].message")
                                .value(
                                        "Der Name darf nicht leer sein."
                                )
                );

        verify(registrationService, never())
                .register(
                        any(RegistrationRequest.class)
                );
    }

    @Test
    void shouldReturnUnprocessableContentForInvalidEmail()
            throws Exception {
        String requestBody = """
                {
                  "name": "Max Mustermann",
                  "email": "keine-gueltige-email",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
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
                        jsonPath("$.status")
                                .value(422)
                )
                .andExpect(
                        jsonPath("$.code").value(
                                "INVALID_REGISTRATION_REQUEST"
                        )
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Die Registrierungsdaten sind ungültig."
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].field")
                                .value("email")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].code")
                                .value("INVALID_EMAIL")
                )
                .andExpect(
                        jsonPath("$.fieldErrors[0].message")
                                .value(
                                        "Die E-Mail-Adresse ist ungültig."
                                )
                );

        verify(registrationService, never())
                .register(
                        any(RegistrationRequest.class)
                );
    }

    @Test
    void shouldReturnConflictForAlreadyRegisteredEmail()
            throws Exception {
        when(registrationService.register(
                any(RegistrationRequest.class)
        )).thenThrow(
                new EmailAlreadyExistsException()
        );

        String requestBody = """
                {
                  "name": "Max Mustermann",
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        header().string(
                                CorrelationIdFilter.HEADER_NAME,
                                CORRELATION_ID
                        )
                )
                .andExpect(
                        jsonPath("$.type").value(
                                "https://warp.local/problems/"
                                        + "email-already-exists"
                        )
                )
                .andExpect(
                        jsonPath("$.title")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("EMAIL_ALREADY_EXISTS")
                )
                .andExpect(
                        jsonPath("$.detail").value(
                                "Für diese E-Mail-Adresse existiert "
                                        + "bereits ein Benutzerkonto."
                        )
                )
                .andExpect(
                        jsonPath("$.instance")
                                .value("/internal/v1/users")
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                )
                .andExpect(
                        jsonPath("$.message")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.fieldErrors")
                                .doesNotExist()
                );

        verify(registrationService)
                .register(
                        any(RegistrationRequest.class)
                );
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws Exception {
        RegistrationResponse response =
                new RegistrationResponse(
                        "usr_123",
                        "Max Mustermann",
                        "max@example.de",
                        false,
                        Instant.parse(
                                "2026-10-03T10:30:00Z"
                        )
                );

        when(registrationService.register(
                any(RegistrationRequest.class)
        )).thenReturn(response);

        String requestBody = """
                {
                  "name": "Max Mustermann",
                  "email": "max@example.de",
                  "password": "MeinSicheresPasswort123"
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(
                        header().exists(
                                CorrelationIdFilter.HEADER_NAME
                        )
                );
    }

    @Test
    void shouldReturnBadRequestForMalformedJson()
            throws Exception {
        String malformedRequestBody = """
                {
                  "name": "Max Mustermann",
                  "email":
                }
                """;

        mockMvc.perform(
                        post("/internal/v1/users")
                                .header(
                                        CorrelationIdFilter.HEADER_NAME,
                                        CORRELATION_ID
                                )
                                .header(
                                        InternalServiceAuthenticationFilter
                                                .HEADER_NAME,
                                        INTERNAL_SERVICE_TOKEN
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(malformedRequestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_PROBLEM_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.code").value(
                                "MALFORMED_REQUEST_BODY"
                        )
                )
                .andExpect(
                        jsonPath("$.correlationId")
                                .value(CORRELATION_ID)
                );

        verify(registrationService, never())
                .register(
                        any(RegistrationRequest.class)
                );
    }
}
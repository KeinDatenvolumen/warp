package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.application.exception.RegistrationRejectedException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.dto.InternalCreateSessionRequest;
import de.warp.bff.infrastructure.client.dto.InternalRegistrationRequest;
import de.warp.bff.infrastructure.client.dto.InternalRegistrationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;

class IdentityClientTest {

    private static final String IDENTITY_SERVICE_URL =
            "http://identity-service";

    private MockRestServiceServer mockServer;
    private IdentityClient identityClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder =
                RestClient.builder();

        mockServer = MockRestServiceServer
                .bindTo(restClientBuilder)
                .build();

        identityClient = new IdentityClient(
                restClientBuilder.build(),
                JsonMapper.builder().build(),
                IDENTITY_SERVICE_URL
        );
    }

    @Test
    void shouldRegisterUserThroughIdentityService() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/users"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "name": "Max Mustermann",
                                  "email": "max@example.de",
                                  "password": "MeinSicheresPasswort123"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(HttpStatus.CREATED)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .body(
                                        """
                                        {
                                          "id": "usr_123",
                                          "name": "Max Mustermann",
                                          "email": "max@example.de",
                                          "emailVerified": false,
                                          "createdAt": "2026-10-18T10:00:00Z"
                                        }
                                        """
                                )
                );

        InternalRegistrationResponse response =
                identityClient.register(
                        new InternalRegistrationRequest(
                                "Max Mustermann",
                                "max@example.de",
                                "MeinSicheresPasswort123"
                        )
                );

        assertThat(response.id())
                .isEqualTo("usr_123");
        assertThat(response.emailVerified())
                .isFalse();

        mockServer.verify();
    }

    @Test
    void shouldTranslateInvalidPasswordDuringRegistration() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/users"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.UNPROCESSABLE_CONTENT
                        )
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        """
                                        {
                                          "status": 422,
                                          "code": "INVALID_PASSWORD",
                                          "detail": "Interne Passwortregel"
                                        }
                                        """
                                )
                );

        InternalRegistrationRequest request =
                new InternalRegistrationRequest(
                        "Max Mustermann",
                        "max@example.de",
                        "MeinSicheresPasswort123"
                );

        assertThatThrownBy(
                () -> identityClient.register(request)
        )
                .isInstanceOf(
                        de.warp.bff.application.exception
                                .RegistrationRejectedException.class
                )
                .hasMessage(
                        "Das Passwort erfüllt die Anforderungen nicht."
                );

        mockServer.verify();
    }

    @Test
    void shouldTranslateIdentityServerErrorDuringRegistration() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/users"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        """
                                        {
                                          "status": 500,
                                          "code": "INTERNAL_ERROR",
                                          "detail": "Vertrauliche interne Meldung",
                                          "stackTrace": "darf nicht erscheinen"
                                        }
                                        """
                                )
                );

        InternalRegistrationRequest request =
                new InternalRegistrationRequest(
                        "Max Mustermann",
                        "max@example.de",
                        "MeinSicheresPasswort123"
                );

        assertThatThrownBy(
                () -> identityClient.register(request)
        )
                .isInstanceOf(
                        DownstreamServiceUnavailableException.class
                )
                .hasMessage(
                        "Der Identitätsdienst ist momentan "
                                + "nicht verfügbar. Bitte versuche "
                                + "es später erneut."
                );

        mockServer.verify();
    }

    @Test
    void shouldTranslateExistingEmailDuringRegistration() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/users"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "name": "Max Mustermann",
                                  "email": "max@example.de",
                                  "password": "MeinSicheresPasswort123"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(HttpStatus.CONFLICT)
                                .contentType(
                                        MediaType.APPLICATION_PROBLEM_JSON
                                )
                                .body(
                                        """
                                        {
                                          "type": "https://warp.local/problems/email-already-exists",
                                          "title": "Conflict",
                                          "status": 409,
                                          "code": "EMAIL_ALREADY_EXISTS",
                                          "detail": "Interne Meldung, die nicht nach außen gelangen darf.",
                                          "instance": "/internal/v1/users",
                                          "correlationId": "test-correlation-id",
                                          "fieldErrors": []
                                        }
                                        """
                                )
                );

        InternalRegistrationRequest request =
                new InternalRegistrationRequest(
                        "Max Mustermann",
                        "max@example.de",
                        "MeinSicheresPasswort123"
                );

        assertThatThrownBy(
                () -> identityClient.register(request)
        )
                .isInstanceOf(
                        RegistrationRejectedException.class
                )
                .hasMessage(
                        "Für diese E-Mail-Adresse ist bereits "
                                + "ein Benutzerkonto vorhanden."
                )
                .satisfies(exception -> {
                    RegistrationRejectedException
                            registrationException =
                            (RegistrationRejectedException)
                                    exception;

                    assertThat(
                            registrationException.getStatus()
                    ).isEqualTo(HttpStatus.CONFLICT);

                    assertThat(
                            registrationException.getCode()
                    ).isEqualTo(
                            "EMAIL_ALREADY_EXISTS"
                    );

                    assertThat(
                            registrationException.getFieldErrors()
                    ).isEmpty();
                });

        mockServer.verify();
    }

    @Test
    void shouldTranslateUnauthorizedLoginToInvalidCredentials() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(HttpStatus.UNAUTHORIZED)
                );

        InternalCreateSessionRequest request =
                new InternalCreateSessionRequest(
                        "max@example.de",
                        "FalschesPasswort123"
                );

        assertThatThrownBy(
                () -> identityClient.createSession(request)
        ).isInstanceOf(
                InvalidCredentialsException.class
        );

        mockServer.verify();
    }

    @Test
    void shouldTranslateIdentityServerErrorDuringLogin() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withStatus(
                                HttpStatus.INTERNAL_SERVER_ERROR
                        )
                );

        InternalCreateSessionRequest request =
                new InternalCreateSessionRequest(
                        "max@example.de",
                        "MeinSicheresPasswort123"
                );

        assertThatThrownBy(
                () -> identityClient.createSession(request)
        )
                .isInstanceOf(
                        DownstreamServiceUnavailableException.class
                )
                .hasMessage(
                        "Der Identitätsdienst ist momentan "
                                + "nicht verfügbar. Bitte versuche "
                                + "es später erneut."
                );

        mockServer.verify();
    }

    @Test
    void shouldTranslateUnknownSessionToSessionInvalid() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions/validation"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "sessionToken": "unknown-session-token"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(HttpStatus.NOT_FOUND)
                );

        assertThatThrownBy(
                () -> identityClient.validateSession(
                        "unknown-session-token"
                )
        ).isInstanceOf(
                SessionInvalidException.class
        );

        mockServer.verify();
    }

    @Test
    void shouldTranslateExpiredSessionToSessionInvalid() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions/validation"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "sessionToken": "expired-session-token"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(HttpStatus.GONE)
                );

        assertThatThrownBy(
                () -> identityClient.validateSession(
                        "expired-session-token"
                )
        ).isInstanceOf(
                SessionInvalidException.class
        );

        mockServer.verify();
    }

    @Test
    void shouldTranslateIdentityServerErrorDuringValidation() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions/validation"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().json(
                                """
                                {
                                  "sessionToken": "test-session-token"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(
                                HttpStatus.SERVICE_UNAVAILABLE
                        )
                );

        assertThatThrownBy(
                () -> identityClient.validateSession(
                        "test-session-token"
                )
        ).isInstanceOf(
                DownstreamServiceUnavailableException.class
        );

        mockServer.verify();
    }

    @Test
    void shouldTranslateIdentityServerErrorDuringLogout() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions/invalidation"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        content().json(
                                """
                                {
                                  "sessionToken": "test-session-token"
                                }
                                """
                        )
                )
                .andRespond(
                        withStatus(
                                HttpStatus.BAD_GATEWAY
                        )
                );

        assertThatThrownBy(
                () -> identityClient.invalidateSession(
                        "test-session-token"
                )
        ).isInstanceOf(
                DownstreamServiceUnavailableException.class
        );

        mockServer.verify();
    }

    @Test
    void shouldRejectEmptySuccessfulLoginResponse() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                "",
                                MediaType.APPLICATION_JSON
                        )
                );

        InternalCreateSessionRequest request =
                new InternalCreateSessionRequest(
                        "max@example.de",
                        "MeinSicheresPasswort123"
                );

        assertThatThrownBy(
                () -> identityClient.createSession(request)
        ).isInstanceOf(
                DownstreamServiceUnavailableException.class
        );

        mockServer.verify();
    }

    @Test
    void shouldRejectEmptySuccessfulValidationResponse() {
        mockServer.expect(
                        requestTo(
                                IDENTITY_SERVICE_URL
                                        + "/internal/v1/sessions/validation"
                        )
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(
                        content().json(
                                """
                                {
                                  "sessionToken": "test-session-token"
                                }
                                """
                        )
                )
                .andRespond(
                        withSuccess(
                                "",
                                MediaType.APPLICATION_JSON
                        )
                );

        assertThatThrownBy(
                () -> identityClient.validateSession(
                        "test-session-token"
                )
        ).isInstanceOf(
                DownstreamServiceUnavailableException.class
        );

        mockServer.verify();
    }
}
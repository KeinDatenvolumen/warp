package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import de.warp.bff.application.exception.InvalidCredentialsException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.dto.InternalCreateSessionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                IDENTITY_SERVICE_URL
        );
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
// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): Wetter- & Routen-Plattform
package de.warp.bff.infrastructure.client;

import de.warp.bff.api.dto.FieldErrorResponse;
import de.warp.bff.application.exception.AccountChangeRejectedException;
import de.warp.bff.application.exception.DownstreamServiceUnavailableException;
import de.warp.bff.graphql.SessionInvalidException;
import de.warp.bff.infrastructure.client.dto.InternalUserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IdentityUserClientTest {

    private MockRestServiceServer mockServer;
    private IdentityUserClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        mockServer = MockRestServiceServer.bindTo(builder).build();

        client = new IdentityUserClient(
                builder.build(),
                JsonMapper.builder().build()
        );
    }

    @Test
    void shouldReadUser() {
        mockServer.expect(requestTo("/internal/v1/users/usr_1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "id": "usr_1",
                          "name": "Max Mustermann",
                          "email": "max@example.de",
                          "emailVerified": true,
                          "createdAt": "2026-10-03T10:30:00Z"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        InternalUserResponse user = client.getUser("usr_1");

        assertThat(user.name()).isEqualTo("Max Mustermann");
        assertThat(user.emailVerified()).isTrue();
        mockServer.verify();
    }

    @Test
    void shouldTreatUnknownUserAsInvalidSession() {
        mockServer.expect(requestTo("/internal/v1/users/usr_1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getUser("usr_1"))
                .isInstanceOf(SessionInvalidException.class);
    }

    @Test
    void shouldChangePassword() {
        mockServer.expect(
                        requestTo("/internal/v1/users/usr_1/password-changes")
                )
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(
                        """
                        {
                          "currentPassword": "alt-passwort-123",
                          "newPassword": "neues-passwort-123"
                        }
                        """
                ))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.changePassword(
                "usr_1",
                "alt-passwort-123",
                "neues-passwort-123"
        );

        mockServer.verify();
    }

    @Test
    void shouldMapWrongPasswordToFieldError() {
        mockServer.expect(
                        requestTo("/internal/v1/users/usr_1/password-changes")
                )
                .andRespond(
                        withStatus(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body("{\"status\":403,\"code\":\"INVALID_PASSWORD\"}")
                );

        assertThatThrownBy(() -> client.changePassword("usr_1", "x", "y"))
                .isInstanceOfSatisfying(
                        AccountChangeRejectedException.class,
                        exception -> {
                            assertThat(exception.getStatus())
                                    .isEqualTo(HttpStatus.FORBIDDEN);
                            assertThat(exception.getCode())
                                    .isEqualTo("INVALID_PASSWORD");
                            assertThat(exception.getFieldErrors())
                                    .extracting(FieldErrorResponse::field)
                                    .containsExactly("currentPassword");
                        }
                );
    }

    @Test
    void shouldMapExistingEmailToConflict() {
        mockServer.expect(
                        requestTo("/internal/v1/users/usr_1/email-changes")
                )
                .andRespond(
                        withStatus(HttpStatus.CONFLICT)
                                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                                .body("{\"status\":409,\"code\":\"EMAIL_ALREADY_EXISTS\"}")
                );

        assertThatThrownBy(
                () -> client.changeEmail("usr_1", "neu@example.de", "pw")
        ).isInstanceOfSatisfying(
                AccountChangeRejectedException.class,
                exception -> {
                    assertThat(exception.getStatus())
                            .isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getFieldErrors())
                            .extracting(FieldErrorResponse::field)
                            .containsExactly("newEmail");
                }
        );
    }

    @Test
    void shouldHideUnexpectedDownstreamErrors() {
        mockServer.expect(
                        requestTo("/internal/v1/users/usr_1/email-changes")
                )
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(
                () -> client.changeEmail("usr_1", "neu@example.de", "pw")
        ).isInstanceOf(DownstreamServiceUnavailableException.class);
    }
}
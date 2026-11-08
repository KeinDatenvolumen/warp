// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
package de.warp.bff.infrastructure.client;

import de.warp.bff.api.exception.InvalidEmailVerificationTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("IdentityEmailVerificationClient Unit Tests")
class IdentityEmailVerificationClientTest {

    private MockRestServiceServer mockServer;
    private IdentityEmailVerificationClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new IdentityEmailVerificationClient(builder.build());
    }

    @Test
    @DisplayName("Constructor throws NullPointerException when restClient is null")
    void constructorRejectsNull() {
        assertThatThrownBy(() -> new IdentityEmailVerificationClient(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("identityRestClient darf nicht null sein");
    }

    @Test
    @DisplayName("confirmEmail succeeds on HTTP 200")
    void confirmEmailSuccess() {
        mockServer.expect(requestTo("/internal/v1/email-verifications/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "token": "valid-token-123"
                        }
                        """))
                .andRespond(withSuccess());

        assertThatCode(() -> client.confirmEmail("valid-token-123"))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    @Test
    @DisplayName("confirmEmail throws InvalidEmailVerificationTokenException on HTTP 400")
    void confirmEmailThrowsOnBadRequest400() {
        mockServer.expect(requestTo("/internal/v1/email-verifications/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.confirmEmail("invalid-token"))
                .isInstanceOf(InvalidEmailVerificationTokenException.class);

        mockServer.verify();
    }

    @Test
    @DisplayName("resendVerificationEmail succeeds on HTTP 200")
    void resendVerificationEmailSuccess() {
        mockServer.expect(requestTo("/internal/v1/email-verifications/resend"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "email": "user@example.com"
                        }
                        """))
                .andRespond(withSuccess());

        assertThatCode(() -> client.resendVerificationEmail("user@example.com"))
                .doesNotThrowAnyException();

        mockServer.verify();
    }

    @Test
    @DisplayName("resendVerificationEmail throws IllegalStateException on HTTP error status")
    void resendVerificationEmailThrowsOnError() {
        mockServer.expect(requestTo("/internal/v1/email-verifications/resend"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.resendVerificationEmail("user@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Der Identity-Service konnte die Verifikations-E-Mail nicht erneut versenden");

        mockServer.verify();
    }
}

// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.identity.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalServiceAuthenticationFilterTest {

    private static final String SERVICE_TOKEN =
            "test-internal-service-token";

    private InternalServiceAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthenticationFilter(
                SERVICE_TOKEN
        );
    }

    @Test
    void shouldRejectMissingServiceToken()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/internal/v1/sessions/test-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(response.getStatus())
                .isEqualTo(401);

        assertThat(response.getContentType())
                .startsWith(
                        "application/problem+json"
                );

        assertThat(response.getContentAsString())
                .contains(
                        "\"code\": "
                                + "\"INTERNAL_SERVICE_UNAUTHORIZED\""
                )
                .contains(
                        "\"status\": 401"
                )
                .doesNotContain(SERVICE_TOKEN);

        assertThat(filterChain.getRequest())
                .isNull();
    }

    @Test
    void shouldRejectInvalidServiceToken()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "POST",
                        "/internal/v1/sessions"
                );

        request.addHeader(
                InternalServiceAuthenticationFilter.HEADER_NAME,
                "wrong-service-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(response.getStatus())
                .isEqualTo(401);

        assertThat(response.getContentAsString())
                .contains(
                        "\"code\": "
                                + "\"INTERNAL_SERVICE_UNAUTHORIZED\""
                )
                .doesNotContain("wrong-service-token")
                .doesNotContain(SERVICE_TOKEN);

        assertThat(filterChain.getRequest())
                .isNull();
    }

    @Test
    void shouldAllowValidServiceToken()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/internal/v1/sessions/test-token"
                );

        request.addHeader(
                InternalServiceAuthenticationFilter.HEADER_NAME,
                SERVICE_TOKEN
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(response.getStatus())
                .isEqualTo(200);

        assertThat(filterChain.getRequest())
                .isSameAs(request);
    }

    @Test
    void shouldNotProtectHealthEndpoint()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "/internal/health/readiness"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(filterChain.getRequest())
                .isSameAs(request);
    }

    @Test
    void shouldPassOptionsRequestWithoutToken()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "OPTIONS",
                        "/internal/v1/sessions/test-token"
                );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        assertThat(filterChain.getRequest())
                .isSameAs(request);
    }

    @Test
    void shouldRejectBlankConfiguredServiceToken() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(
                        () ->
                                new InternalServiceAuthenticationFilter(
                                        " "
                                )
                )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessage(
                        "Der interne Service-Token "
                                + "muss konfiguriert sein."
                );
    }
}
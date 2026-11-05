// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.infrastructure.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter =
            new CorrelationIdFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void shouldReuseValidCorrelationId() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "request-123"
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

        assertThat(
                request.getAttribute(
                        CorrelationIdFilter.REQUEST_ATTRIBUTE
                )
        ).isEqualTo("request-123");

        assertThat(
                response.getHeader(
                        CorrelationIdFilter.HEADER_NAME
                )
        ).isEqualTo("request-123");
    }

    @Test
    void shouldGenerateCorrelationIdWhenHeaderIsMissing()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        MockFilterChain filterChain =
                new MockFilterChain();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        Object correlationId = request.getAttribute(
                CorrelationIdFilter.REQUEST_ATTRIBUTE
        );

        assertThat(correlationId)
                .isInstanceOf(String.class);

        assertThat((String) correlationId)
                .isNotBlank()
                .matches(
                        "[0-9a-f]{8}-[0-9a-f]{4}-"
                                + "[0-9a-f]{4}-[0-9a-f]{4}-"
                                + "[0-9a-f]{12}"
                );

        assertThat(
                response.getHeader(
                        CorrelationIdFilter.HEADER_NAME
                )
        ).isEqualTo(correlationId);
    }

    @Test
    void shouldReplaceInvalidCorrelationId()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "invalid correlation id!"
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

        String generatedCorrelationId =
                response.getHeader(
                        CorrelationIdFilter.HEADER_NAME
                );

        assertThat(generatedCorrelationId)
                .isNotBlank()
                .isNotEqualTo("invalid correlation id!");
    }

    @Test
    void shouldRemoveCorrelationIdFromMdcAfterRequest()
            throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                CorrelationIdFilter.HEADER_NAME,
                "request-456"
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

        assertThat(MDC.get("correlationId"))
                .isNull();
    }
}
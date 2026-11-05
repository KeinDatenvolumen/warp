// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class CorrelationIdFilter
        extends OncePerRequestFilter {

    public static final String HEADER_NAME =
            "X-Correlation-Id";

    public static final String REQUEST_ATTRIBUTE =
            CorrelationIdFilter.class.getName()
                    + ".correlationId";

    private static final String MDC_KEY =
            "correlationId";

    private static final Pattern VALID_CORRELATION_ID =
            Pattern.compile("[A-Za-z0-9._-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = determineCorrelationId(
                request.getHeader(HEADER_NAME)
        );

        request.setAttribute(
                REQUEST_ATTRIBUTE,
                correlationId
        );

        response.setHeader(
                HEADER_NAME,
                correlationId
        );

        MDC.put(
                MDC_KEY,
                correlationId
        );

        try {
            filterChain.doFilter(
                    request,
                    response
            );
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String determineCorrelationId(
            String suppliedCorrelationId
    ) {
        if (suppliedCorrelationId != null
                && VALID_CORRELATION_ID
                .matcher(suppliedCorrelationId)
                .matches()) {
            return suppliedCorrelationId;
        }

        return UUID.randomUUID().toString();
    }
}
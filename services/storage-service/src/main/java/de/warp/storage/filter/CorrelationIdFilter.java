// [KI-Log Nr. KI-071] (7a94bc0) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: use new api contract (WARP-005)
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.storage.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Correlation-Id";
    public static final String REQUEST_ATTRIBUTE =
            CorrelationIdFilter.class.getName() + ".correlationId";
    public static final String MDC_KEY = "correlationId";

    private static final Pattern VALID_CORRELATION_ID =
            Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(
                request.getHeader(HEADER_NAME)
        );

        request.setAttribute(REQUEST_ATTRIBUTE, correlationId);
        response.setHeader(HEADER_NAME, correlationId);

        try {
            MDC.put(MDC_KEY, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String resolveCorrelationId(String suppliedCorrelationId) {
        if (suppliedCorrelationId != null
                && VALID_CORRELATION_ID.matcher(suppliedCorrelationId).matches()) {
            return suppliedCorrelationId;
        }

        return UUID.randomUUID().toString();
    }
}
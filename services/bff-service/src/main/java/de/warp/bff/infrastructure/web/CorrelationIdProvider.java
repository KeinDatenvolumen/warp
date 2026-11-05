// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.infrastructure.web;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
public class CorrelationIdProvider {

    public String currentCorrelationId() {
        RequestAttributes requestAttributes =
                RequestContextHolder.getRequestAttributes();

        if (requestAttributes
                instanceof ServletRequestAttributes servletAttributes) {
            Object correlationId = servletAttributes
                    .getRequest()
                    .getAttribute(
                            CorrelationIdFilter.REQUEST_ATTRIBUTE
                    );

            if (correlationId instanceof String value
                    && !value.isBlank()) {
                return value;
            }
        }

        return UUID.randomUUID().toString();
    }
}
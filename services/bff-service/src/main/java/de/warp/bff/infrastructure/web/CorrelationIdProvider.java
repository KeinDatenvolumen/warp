// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
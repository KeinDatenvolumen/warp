// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.bff.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class InternalServiceAuthenticationInterceptor
        implements ClientHttpRequestInterceptor {

    public static final String HEADER_NAME =
            "X-WaRP-Service-Token";

    private final String serviceToken;

    public InternalServiceAuthenticationInterceptor(
            @Value("${warp.security.internal-service-token}")
            String serviceToken
    ) {
        if (serviceToken == null || serviceToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Der interne Service-Token muss konfiguriert sein."
            );
        }

        this.serviceToken = serviceToken;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        request.getHeaders().set(
                HEADER_NAME,
                serviceToken
        );

        return execution.execute(
                request,
                body
        );
    }
}
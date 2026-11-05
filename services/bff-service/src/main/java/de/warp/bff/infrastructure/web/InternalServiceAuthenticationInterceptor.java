// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
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
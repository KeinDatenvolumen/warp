package de.warp.bff.infrastructure.web;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CorrelationIdClientHttpRequestInterceptor
        implements ClientHttpRequestInterceptor {

    private final CorrelationIdProvider correlationIdProvider;

    public CorrelationIdClientHttpRequestInterceptor(
            CorrelationIdProvider correlationIdProvider
    ) {
        this.correlationIdProvider =
                correlationIdProvider;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        request.getHeaders().set(
                CorrelationIdFilter.HEADER_NAME,
                correlationIdProvider.currentCorrelationId()
        );

        return execution.execute(
                request,
                body
        );
    }
}
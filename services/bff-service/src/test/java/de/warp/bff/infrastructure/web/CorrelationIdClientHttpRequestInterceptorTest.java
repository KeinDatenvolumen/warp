package de.warp.bff.infrastructure.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdClientHttpRequestInterceptorTest {

    private CorrelationIdProvider correlationIdProvider;

    private CorrelationIdClientHttpRequestInterceptor
            interceptor;

    @BeforeEach
    void setUp() {
        correlationIdProvider =
                mock(CorrelationIdProvider.class);

        interceptor =
                new CorrelationIdClientHttpRequestInterceptor(
                        correlationIdProvider
                );
    }

    @Test
    void shouldForwardCurrentCorrelationId()
            throws IOException {
        when(correlationIdProvider.currentCorrelationId())
                .thenReturn("request-123");

        TestHttpRequest request =
                new TestHttpRequest();

        byte[] body = new byte[0];

        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);

        ClientHttpResponse response =
                mock(ClientHttpResponse.class);

        when(execution.execute(
                any(HttpRequest.class),
                same(body)
        )).thenReturn(response);

        ClientHttpResponse actualResponse =
                interceptor.intercept(
                        request,
                        body,
                        execution
                );

        assertThat(
                request.getHeaders().getFirst(
                        CorrelationIdFilter.HEADER_NAME
                )
        ).isEqualTo("request-123");

        assertThat(actualResponse)
                .isSameAs(response);

        verify(execution)
                .execute(
                        request,
                        body
                );
    }

    private static final class TestHttpRequest
            implements HttpRequest {

        private final org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.GET;
        }

        @Override
        public URI getURI() {
            return URI.create(
                    "http://identity-service/internal/v1/sessions"
            );
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public java.util.Map<String, Object> getAttributes() {
            return new java.util.HashMap<>();
        }
    }
}
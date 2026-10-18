package de.warp.bff.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalServiceAuthenticationInterceptorTest {

    @Test
    void shouldSetInternalServiceToken() throws IOException {
        InternalServiceAuthenticationInterceptor interceptor =
                new InternalServiceAuthenticationInterceptor(
                        "test-service-token"
                );

        TestHttpRequest request =
                new TestHttpRequest();

        byte[] body = new byte[0];

        ClientHttpRequestExecution execution =
                mock(ClientHttpRequestExecution.class);

        ClientHttpResponse response =
                mock(ClientHttpResponse.class);

        when(execution.execute(
                same(request),
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
                        InternalServiceAuthenticationInterceptor
                                .HEADER_NAME
                )
        ).isEqualTo("test-service-token");

        assertThat(actualResponse)
                .isSameAs(response);

        verify(execution)
                .execute(request, body);
    }

    @Test
    void shouldRejectBlankInternalServiceToken() {
        assertThatThrownBy(
                () -> new InternalServiceAuthenticationInterceptor(
                        " "
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "Der interne Service-Token muss konfiguriert sein."
                );
    }

    private static final class TestHttpRequest
            implements HttpRequest {

        private final org.springframework.http.HttpHeaders headers =
                new org.springframework.http.HttpHeaders();

        private final Map<String, Object> attributes =
                new HashMap<>();

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.POST;
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
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
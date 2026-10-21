package de.warp.bff.infrastructure.configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.warp.bff.infrastructure.web.CorrelationIdClientHttpRequestInterceptor;
import de.warp.bff.infrastructure.web.InternalServiceAuthenticationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {

    @Bean(destroyMethod = "close")
    ExecutorService healthCheckExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    RestClient serviceHealthRestClient(
            @Value("${warp.health.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.health.read-timeout}")
            Duration readTimeout,
            CorrelationIdClientHttpRequestInterceptor
                    correlationIdInterceptor,
            InternalServiceAuthenticationInterceptor
                    serviceAuthenticationInterceptor
    ) {
        return createRestClient(
                connectTimeout,
                readTimeout,
                null,
                correlationIdInterceptor,
                serviceAuthenticationInterceptor
        );
    }

    @Bean
    RestClient routingGeocodingRestClient(
            @Value("${warp.services.routing.url}")
            String routingServiceUrl,
            @Value("${warp.geocoding.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.geocoding.read-timeout}")
            Duration readTimeout
    ) {
        return createRestClient(
                connectTimeout,
                readTimeout,
                routingServiceUrl
        );
    }

    @Bean
    RestClient identityRestClient(
            @Value("${warp.health.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.health.read-timeout}")
            Duration readTimeout,
            @Value("${warp.services.identity.url}")
            String identityServiceUrl,
            CorrelationIdClientHttpRequestInterceptor
                    correlationIdInterceptor,
            InternalServiceAuthenticationInterceptor
                    serviceAuthenticationInterceptor
    ) {
        return createRestClient(
                connectTimeout,
                readTimeout,
                identityServiceUrl,
                correlationIdInterceptor,
                serviceAuthenticationInterceptor
        );
    }

    private RestClient createRestClient(
            Duration connectTimeout,
            Duration readTimeout,
            String baseUrl,
            ClientHttpRequestInterceptor... interceptors
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(requestFactory);

        if (baseUrl != null) {
            builder.baseUrl(removeTrailingSlash(baseUrl));
        }

        for (ClientHttpRequestInterceptor interceptor : interceptors) {
            builder.requestInterceptor(interceptor);
        }

        return builder.build();
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(
                    0,
                    value.length() - 1
            );
        }

        return value;
    }
}
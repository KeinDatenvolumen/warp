package de.warp.bff.infrastructure.configuration;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.warp.bff.infrastructure.web
        .CorrelationIdClientHttpRequestInterceptor;
import de.warp.bff.infrastructure.web
        .InternalServiceAuthenticationInterceptor;
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
                null,
                connectTimeout,
                readTimeout,
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
            Duration readTimeout,
            CorrelationIdClientHttpRequestInterceptor
                    correlationIdInterceptor,
            InternalServiceAuthenticationInterceptor
                    serviceAuthenticationInterceptor
    ) {
        return createRestClient(
                routingServiceUrl,
                connectTimeout,
                readTimeout,
                correlationIdInterceptor,
                serviceAuthenticationInterceptor
        );
    }

    @Bean
    RestClient routingServiceRestClient(
            @Value("${warp.services.routing.url}")
            String routingServiceUrl,
            @Value("${warp.routing.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.routing.read-timeout}")
            Duration readTimeout,
            CorrelationIdClientHttpRequestInterceptor
                    correlationIdInterceptor,
            InternalServiceAuthenticationInterceptor
                    serviceAuthenticationInterceptor
    ) {
        return createRestClient(
                routingServiceUrl,
                connectTimeout,
                readTimeout,
                correlationIdInterceptor,
                serviceAuthenticationInterceptor
        );
    }

    @Bean
    RestClient identityRestClient(
            @Value("${warp.services.identity.url}")
            String identityServiceUrl,
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
                identityServiceUrl,
                connectTimeout,
                readTimeout,
                correlationIdInterceptor,
                serviceAuthenticationInterceptor
        );
    }

    private RestClient createRestClient(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
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

        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(removeTrailingSlash(baseUrl));
        }

        for (ClientHttpRequestInterceptor interceptor : interceptors) {
            builder.requestInterceptor(interceptor);
        }

        return builder.build();
    }

    private String removeTrailingSlash(String value) {
        String normalizedValue = value.strip();

        while (normalizedValue.length() > 1
                && normalizedValue.endsWith("/")) {
            normalizedValue = normalizedValue.substring(
                    0,
                    normalizedValue.length() - 1
            );
        }

        return normalizedValue;
    }

    @Bean
    RestClient storageRestClient(
            @Value("${warp.services.storage.url}")
            String storageServiceUrl,
            @Value("${warp.storage.connect-timeout:2s}")
            Duration connectTimeout,
            @Value("${warp.storage.read-timeout:5s}")
            Duration readTimeout,
            CorrelationIdClientHttpRequestInterceptor
                    correlationIdInterceptor,
            InternalServiceAuthenticationInterceptor
                    serviceAuthenticationInterceptor
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(storageServiceUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(correlationIdInterceptor)
                .requestInterceptor(serviceAuthenticationInterceptor)
                .build();
    }
}
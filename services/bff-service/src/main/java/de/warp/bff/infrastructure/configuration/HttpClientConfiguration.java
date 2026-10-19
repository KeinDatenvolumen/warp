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
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor(
                        correlationIdInterceptor
                )
                .requestInterceptor(
                        serviceAuthenticationInterceptor
                )
                .build();
    }
    @Bean
    RestClient routingGeocodingRestClient(
            @Value("${warp.services.routing.url}") String baseUrl,
            @Value("${warp.geocoding.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.geocoding.read-timeout}")
            Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
    @Bean
    RestClient routingGeocodingRestClient(
            @Value("${warp.services.routing.url}") String baseUrl,
            @Value("${warp.geocoding.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.geocoding.read-timeout}")
            Duration readTimeout
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
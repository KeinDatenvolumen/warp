package de.warp.routing.infrastructure.configuration;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfiguration {

    @Bean
    RestClient mapTilerRestClient(
            @Value("${warp.geocoding.maptiler.base-url}") String baseUrl,
            @Value("${warp.geocoding.maptiler.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.geocoding.maptiler.read-timeout}")
            Duration readTimeout
    ) {
        return createRestClient(
                baseUrl,
                connectTimeout,
                readTimeout
        );
    }

    @Bean
    RestClient openRouteServiceRestClient(
            @Value("${warp.routing.openrouteservice.base-url}")
            String baseUrl,
            @Value("${warp.routing.openrouteservice.connect-timeout}")
            Duration connectTimeout,
            @Value("${warp.routing.openrouteservice.read-timeout}")
            Duration readTimeout
    ) {
        return createRestClient(
                baseUrl,
                connectTimeout,
                readTimeout
        );
    }

    private RestClient createRestClient(
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory();

        requestFactory.setReadTimeout(readTimeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
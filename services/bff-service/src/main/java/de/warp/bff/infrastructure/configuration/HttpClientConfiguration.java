// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-057] (efe88f9) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add mailpit and improved status request (WARP-001)
// [KI-Log Nr. KI-099] (21f31de) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-006: harden BFF session management
// [KI-Log Nr. KI-101] (c1e1a5c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-133] (943d006) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: implement MapTiler place geocoding (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-142] (e2b3544) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: remove duplicate rest client (WARP-010)
// [KI-Log Nr. KI-143] (2e513c7) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix: rest client configuration (WARP-033)
// [KI-Log Nr. KI-152] (2436af2) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-162] (6e98a51) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-165] (fdf6e6e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat(WARP-007): implement email verification flow
// [KI-Log Nr. KI-178] (503803e) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 add BFF routing client
// [KI-Log Nr. KI-184] (d5e294c) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(WARP-020, WARP-021): implement saved routes backend and frontend
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-226] (15ee1f2) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feature/WARP-014 add mcp/ai integration, better frontend, rebasing with life
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
    RestClient weatherServiceRestClient(
            @Value("${warp.services.weather.url}")
            String weatherServiceUrl,
            @Value("${warp.weather.connect-timeout:2s}")
            Duration connectTimeout,
            @Value("${warp.weather.read-timeout:25s}")
            Duration readTimeout,
            CorrelationIdClientHttpRequestInterceptor
                    correlationIdInterceptor,
            InternalServiceAuthenticationInterceptor
                    serviceAuthenticationInterceptor
    ) {
        return createRestClient(
                weatherServiceUrl,
                connectTimeout,
                readTimeout,
                correlationIdInterceptor,
                serviceAuthenticationInterceptor
        );
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
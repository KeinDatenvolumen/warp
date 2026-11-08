// [KI-Log Nr. KI-122] (bb6781c) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-136] (b99ef77) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): fix geocoding architecture and validation (WARP-010)
// [KI-Log Nr. KI-153] (6e9f413) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 configure OpenRouteService client
// [KI-Log Nr. KI-169] (1b02585) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): WARP-012 configure OpenRouteService client
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
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
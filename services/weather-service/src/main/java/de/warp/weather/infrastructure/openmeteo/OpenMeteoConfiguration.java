// [KI-Log Nr. KI-194] (7710846) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: initial commit for the weather service. first connection to openmeteo. (WARP-014)
// [KI-Log Nr. KI-195] (d93a0ce) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat(weather): implement route weather segmentation, 60m cache, scoring and BFF integration (WARP-014)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
package de.warp.weather.infrastructure.openmeteo;

import de.warp.weather.application.WeatherCacheProperties;
import de.warp.weather.application.WeatherScoringProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;

@Configuration
@EnableConfigurationProperties({
        OpenMeteoProperties.class,
        WeatherCacheProperties.class,
        WeatherScoringProperties.class
})
public class OpenMeteoConfiguration {

    @Bean
    public Clock weatherClock() {
        return Clock.systemUTC();
    }

    @Bean
    public RestClient openMeteoRestClient(
            OpenMeteoProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(
                properties.readTimeout()
        );

        return RestClient.builder()
                .baseUrl(
                        properties.baseUrl().toString()
                )
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }
}

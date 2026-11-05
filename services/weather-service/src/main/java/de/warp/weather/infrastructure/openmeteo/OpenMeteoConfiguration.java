// [KI-Log Nr. KI-16] KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): 101-Stufen-Wetterscore, Segmentierung & Abfahrtsoptimierung (WARP-014..019)
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

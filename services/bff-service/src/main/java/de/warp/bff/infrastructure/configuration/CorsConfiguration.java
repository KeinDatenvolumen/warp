// [KI-Log Nr. KI-04] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): GraphQL BFF & Apollo Client (WARP-003/004)
package de.warp.bff.infrastructure.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private static final String LOCAL_FRONTEND_URL =
            "http://localhost:5173";

    private static final String LOCAL_SWAGGER_UI_URL =
            "http://localhost:8088";

    private final String frontendUrl;

    private static final String ALTAIR_GRAPHQL = "chrome-extension://flnheeellpciglgpaodhkhmapeljopja";

    public CorsConfiguration(
            @Value("${warp.frontend.url}") String frontendUrl
    ) {
        this.frontendUrl = removeTrailingSlash(frontendUrl);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        configureCorsMapping(registry, "/api/**");
        configureCorsMapping(registry, "/graphql");
    }

    private void configureCorsMapping(
            CorsRegistry registry,
            String path
    ) {
        registry.addMapping(path)
                .allowedOrigins(
                        frontendUrl,
                        LOCAL_FRONTEND_URL,
                        LOCAL_SWAGGER_UI_URL,
                        ALTAIR_GRAPHQL
                )
                .allowedMethods(
                        "GET",
                        "POST",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
    }
}
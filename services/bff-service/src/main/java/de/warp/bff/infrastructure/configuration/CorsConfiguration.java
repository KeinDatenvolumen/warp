// [KI-Log Nr. KI-003] (39894d5) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: initialize Deployment and Health Endpoints for Services. Frontend to examine health status of services (WARP-001)
// [KI-Log Nr. KI-033] (7bdabfa) KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): feat: add graphql to backend and frontend. Initialize dict for SPA (WARP-003)
// [KI-Log Nr. KI-239] (9f705ff) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): docs & chore: add fallstudie documentation, ki logs, bootRun and flyway dev fixes (WARP-034)
// [KI-Log Nr. KI-245] (session) KI-generiert (Google Antigravity (Gemini 3.8 Flash High)): feat: pending_email, live session verification, service token auth, 10s AI timeout & full commit KI-Log (WARP-007/014/033/034)
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
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "https://*.herokuapp.com",

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
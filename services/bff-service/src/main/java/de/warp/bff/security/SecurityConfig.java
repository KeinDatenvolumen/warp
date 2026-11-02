package de.warp.bff.security;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SessionAuthenticationFilter
            sessionAuthenticationFilter;

    private final JsonMapper jsonMapper;

    public SecurityConfig(
            SessionAuthenticationFilter sessionAuthenticationFilter,
            JsonMapper jsonMapper
    ) {
        this.sessionAuthenticationFilter =
                sessionAuthenticationFilter;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) {
        CookieCsrfTokenRepository csrfTokenRepository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieCustomizer(customizer -> customizer
                .sameSite("None")
                .secure(true)
                .path("/")
        );

        GraphQlOperationClassificationFilter
                graphQlOperationClassificationFilter =
                new GraphQlOperationClassificationFilter(
                        jsonMapper
                );

        http
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().permitAll()
                )
                .csrf(csrf -> csrf
                        .csrfTokenRepository(
                                csrfTokenRepository
                        )
                        .ignoringRequestMatchers(
                                "/api/v1/sessions",
                                "/api/v1/users",
                                "/api/v1/routes/weather-optimization",
                                "/api/v1/routes/weather-ai-analysis"
                        )
                        .ignoringRequestMatchers(
                                request -> Boolean.TRUE.equals(
                                        request.getAttribute(
                                                GraphQlOperationClassificationFilter
                                                        .QUERY_REQUEST_ATTRIBUTE
                                        )
                                )
                        )
                )
                .addFilterBefore(
                        graphQlOperationClassificationFilter,
                        CsrfFilter.class
                )
                .addFilterBefore(
                        sessionAuthenticationFilter,
                        AnonymousAuthenticationFilter.class
                );

        return http.build();
    }
}